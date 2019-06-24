use crate::error::PrismaError;
use crate::PrismaResult;
use core::query_ir::*;
use graphql_parser::query::{Definition, Document, OperationDefinition};

/// Protocol adapter for GraphQL -> Query Document
pub struct GraphQLProtocolAdapter;

impl GraphQLProtocolAdapter {
    pub fn convert(gql_doc: Document) -> PrismaResult<QueryDocument> {
        let operations: Vec<Operation> = gql_doc
            .definitions
            .into_iter()
            .map(|def| {
                //
                unimplemented!()
            })
            .collect::<PrismaResult<Vec<Operation>>>()?;

        Ok(QueryDocument { operations })
    }

    fn convert_definition(def: Definition) -> PrismaResult<Operation> {
        match def {
            Definition::Fragment(f) => Err(PrismaError::UnsupportedFeatureError(
                "Fragment definition",
                format!("Fragment '{}', at position {}.", f.name, f.position),
            )),
            Definition::Operation(op) => match op {
                OperationDefinition::Subscription(s) => Err(PrismaError::UnsupportedFeatureError(
                    "Subscription query",
                    format!("At position {}.", s.position),
                )),
                OperationDefinition::SelectionSet(s) => unimplemented!(),
                OperationDefinition::Query(q) => unimplemented!(),
                OperationDefinition::Mutation(m) => unimplemented!(),
            },
        }
    }
}
