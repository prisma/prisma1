use std::sync::{Arc, Weak};

use crate::{error::Error, models::prelude::*, PrismaResult};

use once_cell::unsync::OnceCell;

pub type SchemaRef = Arc<Schema>;
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
    pub project: ProjectWeakRef,
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

impl SchemaTemplate {
    pub fn build(self, project: ProjectWeakRef) -> SchemaRef {
        let schema = Arc::new(Schema {
            models: OnceCell::new(),
            project: project,
            relations: self.relations,
            enums: self.enums,
            version: self.version,
        });

        let models = self
            .models
            .into_iter()
            .map(|mt| mt.build(Arc::downgrade(&schema)))
            .collect();

        // Models will not be set before this, look above! No panic.
        schema.models.set(models).unwrap();

        schema
    }
}

impl Schema {
    pub fn find_model(&self, name: &str) -> PrismaResult<ModelRef> {
        self.models
            .get()
            .and_then(|models| models.iter().find(|model| model.db_name() == name))
            .cloned()
            .ok_or_else(|| Error::InvalidInputError(format!("Model not found: {}", name)))
    }

    pub fn is_legacy(&self) -> bool {
        self.version.is_none()
    }

    pub fn with_project<F, T>(&self, f: F) -> T
    where
        F: FnOnce(Arc<Project>) -> T,
    {
        match self.project.upgrade(){
            Some(model) => f(model),
            None => panic!(
                "Project does not exist anymore. Parent project is deleted without deleting the child schema."
            )
        }
    }
}
