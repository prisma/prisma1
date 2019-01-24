pub use crate::schema::*;

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Project {
    pub id: String,
    pub schema: Schema,
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

/// Timeout in seconds.
#[derive(Serialize, Deserialize, Debug)]
pub struct Revision(u32);

impl Default for Revision {
    fn default() -> Self {
        Revision(1)
    }
}

/// Timeout in seconds.
#[derive(Serialize, Deserialize, Debug)]
pub struct DefaultTrue(bool);

impl Default for DefaultTrue {
    fn default() -> Self {
        DefaultTrue(true)
    }
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Function {
    pub name: String,
    pub is_active: bool,
    pub delivery: FunctionDelivery,
    pub type_code: FunctionType,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum FunctionDelivery {
    WebhookDelivery,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum FunctionType {
    ServerSideSubscription,
}

#[derive(Default, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProjectManifestation {
    database: Option<String>,
    schema: Option<String>,
}
