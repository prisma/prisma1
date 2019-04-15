//! Prisma query AST module

use connector::{filter::NodeSelector, QueryArguments};
use prisma_models::prelude::*;

#[derive(Debug, Clone)]
pub enum PrismaQuery {
    RecordQuery(RecordQuery),
    MultiRecordQuery(MultiRecordQuery),
    RelatedRecordQuery(RelatedRecordQuery),
    MultiRelatedRecordQuery(MultiRelatedRecordQuery),
}

#[derive(Debug, Clone)]
pub struct RecordQuery {
    pub name: String,
    pub selector: NodeSelector,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug, Clone)]
pub struct MultiRecordQuery {
    pub name: String,
    pub model: ModelRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug, Clone)]
pub struct RelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug, Clone)]
pub struct MultiRelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}
