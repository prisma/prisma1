pub use crate::schema::*;
use std::{cell::RefCell, rc::Rc};

pub trait Renameable {
    fn db_name(&self) -> &str;
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProjectTemplate {
    pub id: String,
    pub schema: SchemaTemplate,
    pub functions: Vec<Function>,

    #[serde(default)]
    pub manifestation: ProjectManifestation,

    #[serde(default)]
    pub revision: Revision,

    #[serde(default)]
    pub secrets: Vec<String>,

    #[serde(default)]
    pub allow_queries: DefaultTrue,

    #[serde(default)]
    pub allow_mutations: DefaultTrue,
}

#[derive(Debug)]
pub struct Project {
    pub id: String,
    pub schema: Rc<RefCell<Schema>>,
    pub functions: Vec<Function>,
    pub manifestation: ProjectManifestation,
    pub revision: Revision,
    pub secrets: Vec<String>,
    pub allow_queries: DefaultTrue,
    pub allow_mutations: DefaultTrue,
}

impl Into<Project> for ProjectTemplate {
    fn into(self) -> Project {
        Project {
            id: self.id,
            schema: self.schema.into(),
            functions: self.functions,
            manifestation: self.manifestation,
            revision: self.revision,
            secrets: self.secrets,
            allow_queries: self.allow_queries,
            allow_mutations: self.allow_mutations,
        }
    }
}

impl Renameable for Project {
    fn db_name(&self) -> &str {
        match self.manifestation {
            ProjectManifestation {
                database: _,
                schema: Some(ref schema),
            } => schema,
            ProjectManifestation {
                database: Some(ref database),
                schema: _,
            } => database,
            _ => self.id.as_ref(),
        }
    }
}

/// Timeout in seconds.
#[derive(Deserialize, Debug)]
pub struct Revision(u32);

impl Default for Revision {
    fn default() -> Self {
        Revision(1)
    }
}

/// Timeout in seconds.
#[derive(Deserialize, Debug)]
pub struct DefaultTrue(bool);

impl Default for DefaultTrue {
    fn default() -> Self {
        DefaultTrue(true)
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Function {
    pub name: String,
    pub is_active: bool,
    pub delivery: FunctionDelivery,
    pub type_code: FunctionType,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum FunctionDelivery {
    WebhookDelivery,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum FunctionType {
    ServerSideSubscription,
}

#[derive(Default, Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProjectManifestation {
    database: Option<String>,
    schema: Option<String>,
}
