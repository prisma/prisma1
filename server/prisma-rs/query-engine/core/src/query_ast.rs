//! Prisma read query AST module

use connector::{filter::NodeSelector, QueryArguments};
use prisma_models::prelude::*;

#[derive(Debug, Clone)]
pub enum ReadQuery {
    RecordQuery(RecordQuery),
    ManyRecordsQuery(ManyRecordsQuery),
    RelatedRecordQuery(RelatedRecordQuery),
    ManyRelatedRecordsQuery(ManyRelatedRecordsQuery),
}

#[derive(Debug, Clone)]
pub struct RecordQuery {
    pub name: String,
    pub selector: NodeSelector,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub fields: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct ManyRecordsQuery {
    pub name: String,
    pub model: ModelRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub fields: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct RelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub fields: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct ManyRelatedRecordsQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub fields: Vec<String>,
}
