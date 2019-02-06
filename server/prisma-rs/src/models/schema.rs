use std::rc::{Rc, Weak};

use crate::models::{ModelRef, ModelTemplate};
use once_cell::unsync::OnceCell;

pub type SchemaRef = Rc<Schema>;
pub type SchemaWeakRef = Weak<Schema>;

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
    pub models: OnceCell<Vec<ModelRef>>,
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
        let schema = Rc::new(Schema {
            models: OnceCell::new(),
            relations: self.relations,
            enums: self.enums,
            version: self.version,
        });

        let models = self
            .models
            .into_iter()
            .map(|mt| mt.build(Rc::downgrade(&schema)))
            .collect();

        // Models will not be set before this, look above! No panic.
        schema.models.set(models).unwrap();

        schema
    }
}

impl Schema {
    pub fn find_model(&self, name: &str) -> Option<ModelRef> {
        self.models
            .get()
            .and_then(|models| models.iter().find(|model| model.name == name))
            .cloned()
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
    fn test_schema_json_deserialize_legacy() {
        let template: SchemaTemplate =
            serde_json::from_reader(File::open("./legacy_schema.json").unwrap()).unwrap();

        let schema: SchemaRef = template.into();

        assert!(schema.is_legacy());

        let model = schema.find_model("Subscription").unwrap();

        assert!(model.is_legacy());

        let id_field = model.find_field("id").unwrap();
        assert!(id_field.is_id());
        assert!(!id_field.is_created_at());
        assert!(!id_field.is_updated_at());
        assert!(!id_field.is_writable());

        let title_field = model.find_field("title").unwrap();
        assert!(!title_field.is_id());
        assert!(!title_field.is_created_at());
        assert!(!title_field.is_updated_at());
        assert!(title_field.is_writable());

        let created_at_field = model.find_field("createdAt").unwrap();
        assert!(!created_at_field.is_id());
        assert!(created_at_field.is_created_at());
        assert!(!created_at_field.is_updated_at());
        assert!(!created_at_field.is_writable());

        let updated_at_field = model.find_field("updatedAt").unwrap();
        assert!(!updated_at_field.is_id());
        assert!(!updated_at_field.is_created_at());
        assert!(updated_at_field.is_updated_at());
        assert!(!updated_at_field.is_writable());
    }
}
