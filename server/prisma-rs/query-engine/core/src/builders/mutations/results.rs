//! WriteQuery results are kinda special

use connector::mutaction::{DatabaseMutactionResult, Identifier};
use crate::{WriteQuery, ReadQueryResult, SingleReadQueryResult};
use prisma_models::{SingleNode, Node, PrismaValue};

/// A structure that encodes the results from a database mutation
pub struct WriteQueryResult {
    /// The immediate mutation return
    pub inner: DatabaseMutactionResult,

    /// The WriteQuery is used for all sorts of stuff later
    pub origin: WriteQuery,

    /// Nested mutation results
    pub nested: Vec<WriteQueryResult>,
}

impl WriteQueryResult {

    /// A function that's mostly invoked for `DeleteMany` mutations
    pub fn generate_result(&self) -> Option<ReadQueryResult> {
        match self.inner.identifier {
            Identifier::Count(c) => Some(ReadQueryResult::Single(SingleReadQueryResult {
                name: self.origin.field.alias.as_ref().unwrap_or(&self.origin.field.name).clone(),
                fields: vec!["count".into()],
                scalars: Some(SingleNode::new(Node::new(vec![PrismaValue::Int(c as i64)]), vec!["count".into()])),
                nested: vec![],
                lists: vec![],
                selected_fields: self.origin.model().into(),
            })),
            _ => None
        }
    }
}