//! Simple wrapper for WriteQueries

use crate::{builders::utils, BuilderExt, ManyBuilder, ReadQuery, SingleBuilder};
use connector::mutaction::{
    DatabaseMutactionResult as MutationResult, NestedDatabaseMutaction as NestedMutation,
    TopLevelDatabaseMutaction as RootMutation, Identifier,
};
use graphql_parser::query::Field;
use prisma_models::ModelRef;
use std::sync::Arc;

/// A top-level write query (mutation)
#[derive(Debug, Clone)]
pub struct WriteQuery {
    /// The actual mutation object being built
    pub inner: RootMutation,

    /// Required to create following ReadQuery
    pub field: Field,

    /// Nested mutations
    pub nested: Vec<NestedWriteQuery>,
}

/// Nested mutations are slightly different than top-level mutations.
#[derive(Debug, Clone)]
pub struct NestedWriteQuery {
    /// The nested mutation being built
    pub inner: NestedMutation,

    /// Required to create following ReadQuery
    pub field: Field,

    /// NestedWriteQueries can only have nested children
    pub nested: Vec<NestedWriteQuery>,
}

impl WriteQuery {
    pub fn model(&self) -> ModelRef {
        match self.inner {
            RootMutation::CreateNode(ref node) => Arc::clone(&node.model),
            RootMutation::UpdateNode(ref node) => node.where_.field.model.upgrade().unwrap(),
            RootMutation::DeleteNode(ref node) => node.where_.field.model.upgrade().unwrap(),
            RootMutation::UpsertNode(ref node) => node.where_.field.model.upgrade().unwrap(),
            RootMutation::UpdateNodes(ref nodes) => Arc::clone(&nodes.model),
            RootMutation::DeleteNodes(ref nodes) => Arc::clone(&nodes.model),
            _ => unimplemented!(),
        }
    }

    /// This function generates a pre-fetch `ReadQuery` for appropriate `WriteQuery` types
    pub fn generate_prefetch(&self) -> Option<ReadQuery> {
        match self.inner {
            RootMutation::DeleteNode(_) => SingleBuilder::new()
                .setup(self.model(), &self.field)
                .build()
                .ok()
                .map(|q| ReadQuery::RecordQuery(q)),
            RootMutation::DeleteNodes(_) => ManyBuilder::new()
                .setup(self.model(), &self.field)
                .build()
                .ok()
                .map(|q| ReadQuery::ManyRecordsQuery(q)),
            _ => None,
        }
    }

    /// Generate a `ReadQuery` from the encapsulated `WriteQuery`
    #[warn(warnings)]
    pub fn generate_read(&self, res: MutationResult) -> Option<ReadQuery> {
        let field = match res.identifier {
            Identifier::Id(gql_id) => utils::derive_field(&self.field, self.model(), gql_id),
            _ => unimplemented!(),
        };

        match self.inner {
            // We ignore Deletes because they were already handled
            RootMutation::DeleteNode(_) | RootMutation::DeleteNodes(_) => None,
            RootMutation::CreateNode(_) => SingleBuilder::new()
                .setup(self.model(), &field)
                .build()
                .ok()
                .map(|q| ReadQuery::RecordQuery(q)),

            RootMutation::UpdateNode(_) => SingleBuilder::new()
                .setup(self.model(), &field)
                .build()
                .ok()
                .map(|q| ReadQuery::RecordQuery(q)),
            _ => unimplemented!(),
        }
    }
}

impl NestedWriteQuery {
    pub fn model(&self) -> ModelRef {
        match self.inner {
            NestedMutation::CreateNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            NestedMutation::UpdateNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            NestedMutation::UpsertNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            NestedMutation::DeleteNode(ref node) => node.relation_field.model.upgrade().unwrap(),
            _ => unimplemented!(),
        }
    }
}
