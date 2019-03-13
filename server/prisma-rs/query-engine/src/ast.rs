//! Prisma query AST module

use prisma_models::Model;
use graphql_parser::{self as gql, query::*};
use prisma_models::Schema;
use std::convert::From;


pub enum PrismaQuery {
    Record(RecordQuery),
    MultiRecord(MultiRecordQuery),
    Related(RelatedRecordQuery),
    MultiRelated(MultiRelatedRecordQuery),
}

pub struct RecordQuery {
    model: Model,
    // where: NodeSelector,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct MultiRecordQuery {
    model: Model,
    // args: QueryArguments,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct RelatedRecordQuery {
    // parentField: RelationField,
    // args: QueryArguments,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct MultiRelatedRecordQuery {
    // parentField: RelationField,
    // args: QueryArguments,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}


pub struct QueryBuilder<'schema> {
    pub inner: Document,
    pub schema: &'schema Schema
    pub operation: Option<String>,
}

impl From<QueryBuilder> for Vec<PrismaQuery> {
    fn from(qb: QueryBuilder) -> Self {
        let operation = qb.operation;
        qb.inner.definitions.into_iter().for_each(|d: Definition| match d {
            Definition::Operation(OperationDefinition::SelectionSet(SelectionSet { span, items })) => unimplemented!(),
            Definition::Operation(OperationDefinition::Query(Query {
                position,
                name,
                variable_definitions,
                directives,
                selection_set,
            })) => unimplemented!(),
            _ => unimplemented!(),
        });

        unimplemented!()
    }
}
