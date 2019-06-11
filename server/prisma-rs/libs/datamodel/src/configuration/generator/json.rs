use crate::configuration::Generator;
use serde_json;

pub fn generators_to_json_value(generators: &Vec<Generator>) -> serde_json::Value {
    serde_json::to_value(generators).expect("Failed to render JSON.")
}

pub fn generators_to_json(generators: &Vec<Generator>) -> String {
    serde_json::to_string(generators).expect("Failed to render JSON.")
}

pub fn generators_from_json(json: &str) -> Vec<Generator> {
    serde_json::from_str(json).expect("Failed to parse JSON")
}

pub fn generators_from_json_value(json: serde_json::Value) -> Vec<Generator> {
    serde_json::from_value(json).expect("Failed to parse JSON")
}
