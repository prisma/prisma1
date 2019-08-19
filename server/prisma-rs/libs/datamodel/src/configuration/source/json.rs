use crate::{ast, configuration, get_builtin_sources, StringFromEnvVar};
use serde_json;
use std::collections::HashMap;

#[serde(rename_all = "camelCase")]
#[derive(Debug, serde::Serialize, serde::Deserialize)]
pub struct SourceConfig {
    pub name: String,
    pub connector_type: String,
    pub url: StringFromEnvVar,
    pub config: HashMap<String, String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub documentation: Option<String>,
}

pub fn render_sources_to_json_value(sources: &[Box<configuration::Source>]) -> serde_json::Value {
    let res = sources_to_json_structs(sources);
    serde_json::to_value(&res).expect("Failed to render JSON.")
}

pub fn render_sources_to_json(sources: &[Box<configuration::Source>]) -> String {
    let res = sources_to_json_structs(sources);
    serde_json::to_string_pretty(&res).expect("Failed to render JSON.")
}

fn sources_to_json_structs(sources: &[Box<dyn configuration::Source>]) -> Vec<SourceConfig> {
    let mut res: Vec<SourceConfig> = Vec::new();

    for source in sources {
        res.push(source_to_json_struct(&**source));
    }

    res
}

fn source_to_json_struct(source: &dyn configuration::Source) -> SourceConfig {
    SourceConfig {
        name: source.name().clone(),
        connector_type: String::from(source.connector_type()),
        url: source.url().clone(),
        documentation: source.documentation().clone(),
        config: source.config().clone(),
    }
}

pub fn sources_from_json_value_with_plugins(
    json: serde_json::Value,
    source_definitions: Vec<Box<dyn configuration::SourceDefinition>>,
) -> Vec<Box<configuration::Source>> {
    let json_sources = serde_json::from_value::<Vec<SourceConfig>>(json).expect("Failed to parse JSON");
    sources_from_vec(json_sources, source_definitions)
}

fn sources_from_json(json: &str) -> Vec<Box<dyn configuration::Source>> {
    sources_from_json_with_plugins(json, Vec::new())
}

fn sources_from_json_with_plugins(
    json: &str,
    source_definitions: Vec<Box<dyn configuration::SourceDefinition>>,
) -> Vec<Box<configuration::Source>> {
    let json_sources = serde_json::from_str::<Vec<SourceConfig>>(&json).expect("Failed to parse JSON");
    sources_from_vec(json_sources, source_definitions)
}

fn sources_from_vec(
    sources: Vec<SourceConfig>,
    source_definitions: Vec<Box<dyn configuration::SourceDefinition>>,
) -> Vec<Box<configuration::Source>> {
    let mut res = Vec::new();
    let mut source_loader = configuration::SourceLoader::new();
    for source in get_builtin_sources() {
        source_loader.add_source_definition(source);
    }
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }

    for source in sources {
        res.push(source_from_json(&source, &source_loader))
    }

    res
}

fn source_from_json(source: &SourceConfig, loader: &configuration::SourceLoader) -> Box<configuration::Source> {
    // Loader only works on AST. We should change that.
    // TODO: This is code duplication with source serializer, the format is very similar.
    // Maybe we can impl the Source trait.
    let mut arguments: Vec<ast::Argument> = Vec::new();

    arguments.push(ast::Argument::new_string("provider", &source.connector_type));
    match source.url.from_env_var {
        Some(ref env_var) => {
            let values = vec![ast::Value::StringValue(env_var.to_string(), ast::Span::empty())];
            arguments.push(ast::Argument::new_function("url", "env", values));
        }
        None => {
            arguments.push(ast::Argument::new_string("url", &source.url.value));
        }
    }

    for (key, value) in &source.config {
        arguments.push(ast::Argument::new_string(&key, &value));
    }

    let ast_source = ast::SourceConfig {
        name: ast::Identifier::new(&source.name),
        properties: arguments,
        documentation: source.documentation.clone().map(|text| ast::Comment { text }),
        span: ast::Span::empty(),
    };

    loader
        .load_source(&ast_source)
        .expect("Source loading failed.") // Result
        .expect("Source was disabled. That should not be possible.") // Option
}
