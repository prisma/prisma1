use prisma_models::prelude::{GraphqlId, SingleRecord, ModelRef};
use crate::{filter::RecordFinder, error::ConnectorError};

#[derive(Debug, Clone)]
pub enum Identifier {
    Id(GraphqlId),
    Count(usize),
    Record(SingleRecord),
    None,
}

/// TODO this can likely be removed.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WriteQueryResultType {
    Create,
    Update,
    Delete,
    Many,
    Unit,
}

#[derive(Debug, Clone)]
pub struct WriteQueryResult {
    pub identifier: Identifier,
    pub typ: WriteQueryResultType,
}

impl WriteQueryResult {
    pub fn id(&self) -> &GraphqlId {
        match self.identifier {
            Identifier::Id(ref id) => id,
            _ => panic!("No id defined in WriteQueryResult"),
        }
    }

    pub fn count(&self) -> usize {
        match self.identifier {
            Identifier::Count(count) => count,
            _ => panic!("No count defined in WriteQueryResult"),
        }
    }

    pub fn record(&self) -> &SingleRecord {
        match self.identifier {
            Identifier::Record(ref record) => record,
            _ => panic!("No record defined in WriteQueryResult"),
        }
    }

    /// Attempts to convert a write query result into a RecordFinder required for dependent queries.
    /// Assumes ID field is used as dependent field (which is true for now in the current execution model).
    pub fn to_record_finder(&self, model: ModelRef) -> crate::Result<RecordFinder> {
        let id_field = model.fields().id();

        match &self.identifier {
            Identifier::Id(ref id) => Ok(RecordFinder::new(id_field, id)),
            Identifier::Record(ref r) => r
                .collect_id(&id_field.name)
                .map(|id_val| RecordFinder::new(id_field, id_val))
                .map_err(|err| err.into()),

            other => Err(ConnectorError::InternalConversionError(format!(
                "Impossible conversion of write query result {:?} to RecordFinder.",
                other
            ))),
        }
    }
}
