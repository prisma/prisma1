use crate::{DomainError as Error, DomainResult, GraphqlId, PrismaValue};
use std::convert::TryFrom;

#[derive(Debug, Clone)]
pub struct SingleRecord {
    pub record: Record,
    pub field_names: Vec<String>,
}

impl Into<ManyRecords> for SingleRecord {
    fn into(self) -> ManyRecords {
        ManyRecords {
            records: vec![self.record],
            field_names: self.field_names,
        }
    }
}

impl SingleRecord {
    pub fn new(record: Record, field_names: Vec<String>) -> Self {
        Self { record, field_names }
    }

    pub fn collect_id(&self, id_field: &str) -> DomainResult<GraphqlId> {
        self.record.collect_id(&self.field_names, id_field)
    }

    pub fn get_field_value(&self, field: &str) -> DomainResult<&PrismaValue> {
        self.record.get_field_value(&self.field_names, field)
    }
}

#[derive(Debug, Clone, Default)]
pub struct ManyRecords {
    pub records: Vec<Record>,
    pub field_names: Vec<String>,
}

impl ManyRecords {
    pub fn collect_ids(&self, id_field: &str) -> DomainResult<Vec<GraphqlId>> {
        self.records
            .iter()
            .map(|record| record.collect_id(&self.field_names, id_field).map(|i| i.clone()))
            .collect()
    }

    /// Maps into a Vector of (field_name, value) tuples
    pub fn as_pairs(&self) -> Vec<Vec<(String, PrismaValue)>> {
        self.records
            .iter()
            .map(|record| {
                record
                    .values
                    .iter()
                    .zip(self.field_names.iter())
                    .map(|(value, name)| (name.clone(), value.clone()))
                    .collect()
            })
            .collect()
    }

    /// Reverses the wrapped records in place
    pub fn reverse(&mut self) {
        self.records.reverse();
    }
}

#[derive(Debug, Default, Clone)]
pub struct Record {
    pub values: Vec<PrismaValue>,
    pub parent_id: Option<GraphqlId>,
}

impl Record {
    pub fn new(values: Vec<PrismaValue>) -> Record {
        Record {
            values,
            ..Default::default()
        }
    }

    pub fn collect_id(&self, field_names: &[String], id_field: &str) -> DomainResult<GraphqlId> {
        self.get_field_value(field_names, id_field)
            .and_then(GraphqlId::try_from)
    }

    pub fn get_field_value(&self, field_names: &[String], field: &str) -> DomainResult<&PrismaValue> {
        let index = field_names
            .iter()
            .position(|r| r == field)
            .map(Ok)
            .unwrap_or_else(|| {
                Err(Error::FieldNotFound {
                    name: field.to_string(),
                    model: String::new(),
                })
            })?;

        Ok(&self.values[index])
    }

    pub fn add_parent_id(&mut self, parent_id: GraphqlId) {
        self.parent_id = Some(parent_id);
    }
}
