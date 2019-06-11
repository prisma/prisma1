use super::*;
use serde_json;

#[serde(rename_all = "camelCase")]
#[derive(Debug, serde::Serialize, serde::Deserialize)]
struct SerializeableMcf {
    generators: serde_json::Value,
    datasources: serde_json::Value,
}

fn to_serializeable(mcf: &Configuration) -> SerializeableMcf {
    SerializeableMcf {
        generators: generator::generators_to_json_value(&mcf.generators),
        datasources: source::render_sources_to_json_value(&mcf.datasources),
    }
}

pub fn config_to_mcf_json_value(mcf: &Configuration) -> serde_json::Value {
    serde_json::to_value(&to_serializeable(mcf)).expect("Failed to render JSON.")
}

pub fn config_to_mcf_json(mcf: &Configuration) -> String {
    serde_json::to_string(&to_serializeable(mcf)).expect("Failed to render JSON.")
}

pub fn config_from_mcf_json(json: &str) -> Configuration {
    let mcf: SerializeableMcf = serde_json::from_str(json).expect("Failed to parse JSON.");

    Configuration {
        generators: generator::generators_from_json_value(mcf.generators),
        datasources: source::sources_from_json_value_with_plugins(mcf.datasources, vec![]),
    }
}

pub fn config_from_mcf_json_value(json: serde_json::Value) -> Configuration {
    let mcf: SerializeableMcf = serde_json::from_value(json).expect("Failed to parse JSON.");

    Configuration {
        generators: generator::generators_from_json_value(mcf.generators),
        datasources: source::sources_from_json_value_with_plugins(mcf.datasources, vec![]),
    }
}

pub fn config_from_mcf_json_value_with_plugins(
    json: serde_json::Value,
    plugins: Vec<Box<source::SourceDefinition>>,
) -> Configuration {
    let mcf: SerializeableMcf = serde_json::from_value(json).expect("Failed to parse JSON.");

    Configuration {
        generators: generator::generators_from_json_value(mcf.generators),
        datasources: source::sources_from_json_value_with_plugins(mcf.datasources, plugins),
    }
}
