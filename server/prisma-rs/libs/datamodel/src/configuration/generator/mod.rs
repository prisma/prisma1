mod json;
mod loader;
pub use json::*;
pub use loader::*;

use std::collections::HashMap;

#[serde(rename_all = "camelCase")]
#[derive(Debug, serde::Serialize, serde::Deserialize)]
pub struct Generator {
    name: String,
    provider: String,
    output: Option<String>,
    // Todo: This is a bad choice, PrismaValue is probably better.
    config: HashMap<String, String>,
    documentation: Option<String>,
}
