use std::{cell::RefCell, rc::Rc};

use crate::{
    models::{ModelRef, ModelTemplate},
};

pub type SchemaRef = Rc<RefCell<Schema>>;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SchemaTemplate {
    pub models: Vec<ModelTemplate>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
}

#[derive(Debug)]
pub struct Schema {
    pub models: Vec<ModelRef>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OnDelete {
    SetNull,
    Cascade,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Relation {
    pub name: String,
    pub model_a_id: String,
    pub model_b_id: String,
    pub model_a_on_delete: OnDelete,
    pub model_b_on_delete: OnDelete,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PrismaEnum {
    name: String,
    values: Vec<String>,
}

impl Into<SchemaRef> for SchemaTemplate {
    fn into(self) -> SchemaRef {
        let schema = Rc::new(RefCell::new(Schema {
            models: Vec::new(),
            relations: self.relations,
            enums: self.enums,
            version: self.version,
        }));

        self.models
            .into_iter()
            .for_each(|mt| schema.borrow_mut().models.push(mt.build(schema.clone())));

        schema
    }
}

impl Schema {
    pub fn find_model(&self, name: &str) -> Option<ModelRef> {
        self.models
            .iter()
            .find(|model| model.borrow().name == name)
            .map(|model| model.clone())
    }

    pub fn is_legacy(&self) -> bool {
        self.version.is_none()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json;
    use std::fs::File;

    #[test]
    fn test_schema_json_deserialize() {
        let _: SchemaTemplate =
            serde_json::from_reader(File::open("./example_schema.json").unwrap()).unwrap();
    }
}
