//! WriteQuery results are kinda special

use crate::{ReadQueryResult, SingleReadQueryResult, WriteQueryTree};
use connector::write_query::{Identifier, WriteQueryResult};
use prisma_models::{PrismaValue, Record, SingleRecord};

/// A structure that encodes the results from a database write
pub struct WriteQueryTreeResult {
    /// The immediate write query return
    pub inner: WriteQueryResult,

    /// The WriteQueryTree is used for all sorts of stuff later
    pub origin: WriteQueryTree,
}

impl WriteQueryTreeResult {
    /// A function that's mostly invoked for `DeleteMany` operations
    pub fn generate_result(&self) -> Option<ReadQueryResult> {
        match self.inner.identifier {
            Identifier::Count(c) => Some(ReadQueryResult::Single(SingleReadQueryResult {
                name: self
                    .origin
                    .field
                    .alias
                    .as_ref()
                    .unwrap_or(&self.origin.field.name)
                    .clone(),
                fields: vec!["count".into()],
                scalars: Some(SingleRecord::new(
                    Record::new(vec![PrismaValue::Int(c as i64)]),
                    vec!["count".into()],
                )),
                nested: vec![],
                lists: vec![],
                selected_fields: self.origin.model().into(),
            })),
            _ => None,
        }
    }
}
