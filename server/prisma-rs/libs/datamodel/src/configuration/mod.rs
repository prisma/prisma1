mod generator;
mod json;
mod source;

pub use generator::*;
pub use json::*;
pub use source::*;

use serde::{Deserialize, Serialize};

#[serde(rename_all = "camelCase")]
#[derive(Debug, Serialize, Deserialize)]
pub struct SerializeableMcf {
    generators: serde_json::Value,
    datasources: serde_json::Value,
}

pub struct Configuration {
    pub generators: Vec<Generator>,
    pub datasources: Vec<Box<dyn Source>>,
}

impl Configuration {
    pub fn to_serializeable(&self) -> SerializeableMcf {
        SerializeableMcf {
            generators: generator::generators_to_json_value(&self.generators),
            datasources: source::render_sources_to_json_value(&self.datasources),
        }
    }
}

impl From<SerializeableMcf> for Configuration {
    fn from(mcf: SerializeableMcf) -> Self {
        Self {
            generators: generator::generators_from_json_value(mcf.generators),
            datasources: source::sources_from_json_value_with_plugins(mcf.datasources, vec![]),
        }
    }
}

#[serde(rename_all = "camelCase")]
#[derive(Clone, Debug, serde::Serialize, serde::Deserialize)]
pub struct StringFromEnvVar {
    pub from_env_var: Option<String>,
    pub value: String,
}
