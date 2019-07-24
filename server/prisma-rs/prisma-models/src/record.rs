use crate::{DomainError as Error, DomainResult, GraphqlId, PrismaValue};
use std::convert::TryFrom;

#[derive(Debug, Clone)]
pub struct SingleRecord {
    pub record: Record,
    pub field_names: Vec<String>,
}

impl TryFrom<ManyRecords> for SingleRecord {
    type Error = Error;

    fn try_from(mn: ManyRecords) -> DomainResult<SingleRecord> {
        let field_names = mn.field_names;

        mn.records
            .into_iter()
            .rev()
            .next()
            .map(|record| SingleRecord { record, field_names })
            .ok_or(Error::ConversionFailure("ManyRecords", "SingleRecord"))
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

#[derive(Debug, Clone)]
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

    pub fn collect_id(&self, field_names: &Vec<String>, id_field: &str) -> DomainResult<GraphqlId> {
        self.get_field_value(field_names, id_field).and_then(|raw| GraphqlId::try_from(raw))
    }

    pub fn get_field_value(&self, field_names: &Vec<String>, field: &str) -> DomainResult<&PrismaValue> {
        let index = field_names
            .iter()
            .position(|r| r == field)
            .map(|i| Ok(i))
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
