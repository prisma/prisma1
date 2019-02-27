use std::sync::{Arc, Weak};

use crate::{error::Error, models::prelude::*, PrismaResult};

use once_cell::unsync::OnceCell;

pub type SchemaRef = Arc<Schema>;
pub type SchemaWeakRef = Weak<Schema>;

#[derive(Debug, Deserialize)]
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
    pub relations: OnceCell<Vec<Relation>>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
    #[debug_stub = "#ProjectWeakRef#"]
    pub project: ProjectWeakRef,
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
            relations: OnceCell::new(),
            enums: self.enums,
            version: self.version,
        });

        let models = self
            .models
            .into_iter()
            .map(|mt| mt.build(Arc::downgrade(&schema)))
            .collect();

        let relations = self
            .relations
            .into_iter()
            .map(|rt| rt.build(Arc::downgrade(&schema)))
            .collect();

        schema.models.set(models).unwrap();
        schema.relations.set(relations).unwrap();

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
        F: FnOnce(ProjectRef) -> T,
    {
        match self.project.upgrade(){
            Some(project) => f(project),
            None => panic!(
                "Project does not exist anymore. Parent project is deleted without deleting the child schema."
            )
        }
    }
}
