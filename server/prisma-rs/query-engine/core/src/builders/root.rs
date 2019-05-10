use super::Builder;
use crate::{CoreResult, Query as PrismaQuery, MutationBuilder};
use graphql_parser::query::*;
use prisma_models::InternalDataModelRef;
use std::sync::Arc;

#[derive(Debug)]
pub struct RootBuilder {
    pub query: Document,
    pub internal_data_model: InternalDataModelRef,
    pub operation_name: Option<String>,
}

impl RootBuilder {
    // FIXME: Find op name and only execute op!
    pub fn build(self) -> CoreResult<Vec<PrismaQuery>> {
        self.query
            .definitions
            .iter()
            .map(|d| match d {
                // Query without the explicit "query" before the selection set
                Definition::Operation(OperationDefinition::SelectionSet(SelectionSet { span: _, items })) => {
                    self.build_query(&items)
                }

                // Regular query
                Definition::Operation(OperationDefinition::Query(Query {
                    position: _,
                    name: _,
                    variable_definitions: _,
                    directives: _,
                    selection_set,
                })) => self.build_query(&selection_set.items),

                Definition::Operation(OperationDefinition::Mutation(Mutation {
                    position: _,
                    name: _,
                    variable_definitions: _,
                    directives: _,
                    selection_set,
                })) => self.build_mutation(&selection_set.items),
                _ => unimplemented!(),
            })
            .collect::<CoreResult<Vec<Vec<PrismaQuery>>>>() // Collect all the "query trees"
            .map(|v| v.into_iter().flatten().collect())
    }

    fn build_query(&self, root_fields: &Vec<Selection>) -> CoreResult<Vec<PrismaQuery>> {
        root_fields
            .iter()
            .map(|item| {
                // First query-level fields map to a model in our internal_data_model, either a plural or singular
                match item {
                    Selection::Field(root_field) => Builder::new(Arc::clone(&self.internal_data_model), root_field)?.build().map(|q| PrismaQuery::Read(q)),
                    _ => unimplemented!(),
                }
            })
            .collect()
    }

    /// Mutations do something to the database and then follow-up with a query
    fn build_mutation(&self, root_fields: &Vec<Selection>) -> CoreResult<Vec<PrismaQuery>> {
        root_fields
            .iter()
            .map(|item| {
                match item {
                    Selection::Field(root_field) => MutationBuilder::new(Arc::clone(&self.internal_data_model), root_field).build().map(|q| PrismaQuery::Write(q)),
                    _ => unimplemented!(),
                }
            })
            .collect()
    }
}

trait UuidCheck {
    fn is_uuid(&self) -> bool;
}

impl UuidCheck for String {
    fn is_uuid(&self) -> bool {
        false
    }
}
