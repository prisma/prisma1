mod json;
mod loader;
pub use json::*;
pub use loader::*;

use crate::StringFromEnvVar;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[serde(rename_all = "camelCase")]
#[derive(Debug, Serialize, Deserialize)]
pub struct Generator {
    name: String,
    provider: String,
    output: Option<String>,
    #[serde(default = "Vec::new")]
    platforms: Vec<String>,
    pub pinned_platform: Option<StringFromEnvVar>,
    // Todo: This is a bad choice, PrismaValue is probably better.
    pub config: HashMap<String, String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub documentation: Option<String>,
}
