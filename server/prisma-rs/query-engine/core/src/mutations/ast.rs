//! Simple wrapper for WriteQueries

use connector::mutaction::{TopLevelDatabaseMutaction, NestedDatabaseMutaction};
use graphql_parser::query::Field;
use prisma_models::ModelRef;
use std::sync::Arc;

/// A top-level write query (mutation)
#[derive(Debug, Clone)]
pub struct WriteQuery {
    /// The actual mutation object being built
    pub inner: TopLevelDatabaseMutaction,

    /// Required to create following ReadQuery
    pub field: Field,

    /// Nested mutations
    pub nested: Vec<NestedWriteQuery>,
}

/// Nested mutations are slightly different than top-level mutations.
#[derive(Debug, Clone)]
pub struct NestedWriteQuery {
    /// The nested mutation being built
    pub inner: NestedDatabaseMutaction,

    /// Required to create following ReadQuery
    pub field: Field,

    /// NestedWriteQueries can only have nested children
    pub nested: Vec<NestedWriteQuery>
}

impl WriteQuery {
    pub fn model(&self) -> ModelRef {
        match self.inner {
            TopLevelDatabaseMutaction::CreateNode(ref node) => Arc::clone(&node.model),
            TopLevelDatabaseMutaction::UpdateNode(ref node) => node.where_.field.model.upgrade().unwrap(),
            TopLevelDatabaseMutaction::DeleteNode(ref node) => node.where_.field.model.upgrade().unwrap(),
            TopLevelDatabaseMutaction::UpsertNode(ref node) => node.where_.field.model.upgrade().unwrap(),
            TopLevelDatabaseMutaction::UpdateNodes(ref nodes) => Arc::clone(&nodes.model),
            TopLevelDatabaseMutaction::DeleteNodes(ref nodes) => Arc::clone(&nodes.model),
            _ => unimplemented!(),
        }
    }
}

impl NestedWriteQuery {
    pub fn model(&self) -> ModelRef {
        match self.inner {
            NestedDatabaseMutaction::CreateNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            NestedDatabaseMutaction::UpdateNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            NestedDatabaseMutaction::UpsertNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            NestedDatabaseMutaction::DeleteNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            _ => unimplemented!(),
        }
    }
}