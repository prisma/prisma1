//! Prisma read query AST

use super::ModelExtractor;
use crate::{filter::RecordFinder, QueryArguments};
use prisma_models::prelude::*;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub enum ReadQuery {
    RecordQuery(RecordQuery),
    ManyRecordsQuery(ManyRecordsQuery),
    RelatedRecordsQuery(RelatedRecordsQuery),
}

impl ModelExtractor for ReadQuery {
    fn extract_model(&self) -> Option<ModelRef> {
        match self {
            ReadQuery::RecordQuery(q) => q.record_finder.as_ref().map(|rf| rf.field.model()),
            ReadQuery::ManyRecordsQuery(q) => Some(Arc::clone(&q.model)),
            ReadQuery::RelatedRecordsQuery(q) => Some(q.parent_field.related_model()),
        }
    }
}

#[derive(Debug, Clone)]
pub struct RecordQuery {
    pub name: String,
    pub alias: Option<String>,
    pub record_finder: Option<RecordFinder>,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct ManyRecordsQuery {
    pub name: String,
    pub alias: Option<String>,
    pub model: ModelRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct RelatedRecordsQuery {
    pub name: String,
    pub alias: Option<String>,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<ReadQuery>,
    pub selection_order: Vec<String>,
}
