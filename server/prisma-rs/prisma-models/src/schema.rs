use crate::prelude::*;
use once_cell::sync::OnceCell;
use std::sync::{Arc, Weak};

pub type SchemaRef = Arc<Schema>;
pub type SchemaWeakRef = Weak<Schema>;

#[derive(Debug, Deserialize, Serialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct SchemaTemplate {
    pub models: Vec<ModelTemplate>,
    pub relations: Vec<RelationTemplate>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
}

#[derive(DebugStub)]
pub struct Schema {
    pub models: OnceCell<Vec<ModelRef>>,
    pub relations: OnceCell<Vec<RelationRef>>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
    pub db_name: String,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PrismaEnum {
    pub name: String,
    pub values: Vec<String>,
}

impl SchemaTemplate {
    pub fn build(self, db_name: String) -> SchemaRef {
        let schema = Arc::new(Schema {
            models: OnceCell::new(),
            relations: OnceCell::new(),
            enums: self.enums,
            version: self.version,
            db_name: db_name,
        });

        let models = self
            .models
            .into_iter()
            .map(|mt| mt.build(Arc::downgrade(&schema)))
            .collect();

        schema.models.set(models).unwrap();

        let relations = self
            .relations
            .into_iter()
            .map(|rt| rt.build(Arc::downgrade(&schema)))
            .collect();

        schema.relations.set(relations).unwrap();
        schema
    }
}

impl Schema {
    pub fn models(&self) -> &[ModelRef] {
        self.models.get().unwrap()
    }

    pub fn find_model(&self, name: &str) -> DomainResult<ModelRef> {
        self.models
            .get()
            .and_then(|models| models.iter().find(|model| model.name == name))
            .cloned()
            .ok_or_else(|| DomainError::ModelNotFound { name: name.to_string() })
    }

    pub fn find_relation(&self, name: &str) -> DomainResult<RelationWeakRef> {
        self.relations
            .get()
            .and_then(|relations| relations.iter().find(|relation| relation.name == name))
            .map(|relation| Arc::downgrade(&relation))
            .ok_or_else(|| DomainError::RelationNotFound { name: name.to_string() })
    }

    pub fn is_legacy(&self) -> bool {
        self.version.is_none()
    }
}
