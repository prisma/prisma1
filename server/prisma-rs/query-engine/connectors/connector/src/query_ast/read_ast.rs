//! Prisma read query AST

use crate::{filter::RecordFinder, QueryArguments};
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
    pub record_finder: RecordFinder,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct ManyRecordsQuery {
    pub name: String,
    pub model: ModelRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct RelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct ManyRelatedRecordsQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}
