//! Prisma query AST module

pub enum PrismaQuery {
    Record(RecordQuery),
    MultiRecord(MultiRecordQuery),
    Related(RelatedRecordQuery),
    MultiRelated(MultiRelatedRecordQuery),
}

pub struct RecordQuery {
    // model: Model,
    // where: NodeSelector,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct MultiRecordQuery {
    // model: Model,
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

use graphql_parser::query::Document;
use std::convert::From;

pub struct QueryBuilder {
    pub inner: Document,
    pub operation: String,
}

impl From<QueryBuilder> for PrismaQuery {
    fn from(qb: QueryBuilder) -> Self {
        unimplemented!()
    }
}
