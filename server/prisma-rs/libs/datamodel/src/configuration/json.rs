use super::*;
use serde_json;

pub fn config_to_mcf_json_value(mcf: &Configuration) -> serde_json::Value {
    serde_json::to_value(&mcf.to_serializeable()).expect("Failed to render JSON.")
}

pub fn config_to_mcf_json(mcf: &Configuration) -> String {
    serde_json::to_string(&mcf.to_serializeable()).expect("Failed to render JSON.")
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

    Configuration::from(mcf)
}

pub fn config_from_mcf_json_value_with_plugins(
    json: serde_json::Value,
    plugins: Vec<Box<dyn source::SourceDefinition>>,
) -> Configuration {
    let mcf: SerializeableMcf = serde_json::from_value(json).expect("Failed to parse JSON.");

    Configuration {
        generators: generator::generators_from_json_value(mcf.generators),
        datasources: source::sources_from_json_value_with_plugins(mcf.datasources, plugins),
    }
}
