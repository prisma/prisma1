//! Prisma query AST module

use graphql_parser::{query::*};
//use prisma_models::Model;
use prisma_models::SchemaRef;
use std::convert::From;

pub enum PrismaQuery {
    Record(RecordQuery),
    MultiRecord(MultiRecordQuery),
    Related(RelatedRecordQuery),
    MultiRelated(MultiRelatedRecordQuery),
}

pub struct RecordQuery {
    //model: Model,
    // where: NodeSelector,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct MultiRecordQuery {
    //model: Model,
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

pub struct QueryBuilder {
    pub inner: Document,
    pub schema: SchemaRef,
    pub operation: Option<String>,
}

impl From<QueryBuilder> for Vec<PrismaQuery> {
    fn from(qb: QueryBuilder) -> Self {
        let _operation = qb.operation;
        qb.inner.definitions.into_iter().for_each(|d: Definition| match d {
            Definition::Operation(OperationDefinition::SelectionSet(SelectionSet { span: _, items: _ })) => unimplemented!(),
            Definition::Operation(OperationDefinition::Query(Query {
                position: _,
                name: _,
                variable_definitions: _,
                directives: _,
                selection_set: _,
            })) => unimplemented!(),
            _ => unimplemented!(),
        });

        unimplemented!()
    }
}
