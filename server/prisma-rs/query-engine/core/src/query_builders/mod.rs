//! Query builders module

/// New query building
mod query_builder;
mod read_new;

/// Legacy query buildin
mod read;
mod write;

pub use read::*;
pub use write::*;

use crate::{CoreResult, QuerySchemaRef};
use connector::Query as PrismaQuery;
use graphql_parser::query::*;
use std::sync::Arc;

#[derive(Debug)]
pub struct RootBuilder {
    pub query: Document,
    pub query_schema: QuerySchemaRef,
    pub operation_name: Option<String>,
}

impl RootBuilder {
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
                    Selection::Field(root_field) => ReadQueryBuilder::new(Arc::clone(&self.query_schema), root_field)?
                        .build()
                        .map(|q| PrismaQuery::Read(q)),
                    _ => unimplemented!(),
                }
            })
            .collect()
    }

    /// Mutations do something to the database and then follow-up with a query
    fn build_mutation(&self, root_fields: &Vec<Selection>) -> CoreResult<Vec<PrismaQuery>> {
        // TODO
        //        root_fields
        //            .iter()
        //            .map(|item| match item {
        //                Selection::Field(root_field) => RootWriteQueryBuilder::new(Arc::clone(&self.query_schema), root_field)
        //                    .build()
        //                    .map(|q| PrismaQuery::Write(q)),
        //                _ => unimplemented!(),
        //            })
        //            .collect()
        unimplemented!()
    }
}
