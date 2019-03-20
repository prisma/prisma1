use crate::prelude::{ModelRef, PrismaValue};
use chrono::Utc;
use std::collections::{btree_map::Keys, BTreeMap};

#[derive(Debug, PartialEq, Clone)]
pub struct PrismaArgs {
    args: BTreeMap<String, PrismaValue>,
}

impl PrismaArgs {
    pub fn new(args: BTreeMap<String, PrismaValue>) -> Self {
        Self { args }
    }

    pub fn insert<T, V>(&mut self, key: T, arg: V)
    where
        T: Into<String>,
        V: Into<PrismaValue>,
    {
        self.args.insert(key.into(), arg.into());
    }

    pub fn has_arg_for(&self, field: &str) -> bool {
        self.args.contains_key(field)
    }

    pub fn get_field_value(&self, field: &str) -> Option<&PrismaValue> {
        self.args.get(field)
    }

    pub fn take_field_value(&mut self, field: &str) -> Option<PrismaValue> {
        self.args.remove(field)
    }

    pub fn keys(&self) -> Keys<String, PrismaValue> {
        self.args.keys()
    }

    pub fn add_datetimes(&mut self, model: ModelRef) {
        if !self.args.is_empty() {
            let now = PrismaValue::DateTime(Utc::now());

            match (model.fields().created_at(), model.fields().updated_at()) {
                (Some(created_at), Some(updated_at)) => {
                    self.args.insert(created_at.name.clone(), now.clone());
                    self.args.insert(updated_at.name.clone(), now);
                }
                (Some(created_at), None) => {
                    self.args.insert(created_at.name.clone(), now);
                }
                (None, Some(updated_at)) => {
                    self.args.insert(updated_at.name.clone(), now);
                }
                (None, None) => (),
            }
        }
    }

    pub fn update_datetimes(&mut self, model: ModelRef, list_causes_update: bool) {
        if !self.args.is_empty() || list_causes_update {
            if let Some(field) = model.fields().updated_at() {
                self.args
                    .insert(field.name.to_string(), PrismaValue::DateTime(Utc::now()));
            }
        }
    }
}
