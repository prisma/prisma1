pub use crate::prelude::*;
use once_cell::unsync::OnceCell;
use std::sync::{Arc, Weak};

pub type ProjectRef = Arc<Project>;
pub type ProjectWeakRef = Weak<Project>;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProjectTemplate {
    pub id: String,
    pub schema: SchemaTemplate,

    #[serde(default)]
    pub manifestation: ProjectManifestation,

    // todo: what is this?
    #[serde(default)]
    pub revision: Revision,
}

#[derive(Debug)]
pub struct Project {
    pub id: String,
    pub schema: OnceCell<SchemaRef>,
    pub revision: Revision,
}

impl Into<ProjectRef> for ProjectTemplate {
    fn into(self) -> ProjectRef {
        let db_name = self.db_name();
        let project = Arc::new(Project {
            id: self.id,
            schema: OnceCell::new(),
            revision: self.revision,
        });

        project.schema.set(self.schema.build(db_name)).unwrap();

        project
    }
}

impl ProjectTemplate {
    pub fn db_name(&self) -> String {
        match self.manifestation {
            ProjectManifestation {
                schema: Some(ref schema),
                ..
            } => schema.clone(),
            ProjectManifestation {
                database: Some(ref database),
                ..
            } => database.clone(),
            _ => self.id.clone(),
        }
    }
}

impl Project {
    pub fn schema(&self) -> &Schema {
        self.schema.get().expect("Project has no schema set!")
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
    pub database: Option<String>,
    pub schema: Option<String>,
}

#[cfg(test)]
mod tests {
    use crate::prelude::*;
    use serde_json;
    use std::fs::File;

    #[test]
    fn test_relation_schema() {
        let file = File::open("./relation_schema.json").unwrap();
        let project_template: ProjectTemplate = serde_json::from_reader(file).unwrap();
        let _project: ProjectRef = project_template.into();
        assert!(true)
    }
}
