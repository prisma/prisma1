// Load macros first - Global Macros used for parsing.
// Macro to match all children in a parse tree
#[macro_use]
macro_rules! match_children (
    ($token:ident, $current:ident, $($pattern:pat => $result:expr),*) => (
        // Explicit clone, as into_inner consumes the pair.
        // We only need a reference to the pair later for logging.
        for $current in $token.clone().into_inner() {
            match $current.as_rule() {
                Rule::WHITESPACE => { },
                Rule::COMMENT => { },
                Rule::BLOCK_OPEN => { },
                Rule::BLOCK_CLOSE => { },
                $(
                    $pattern => $result
                ),*
            }
        }
    );
);

// Macro to match the first child in a parse tree
#[macro_use]
macro_rules! match_first (
    ($token:ident, $current:ident, $($pattern:pat => $result:expr),*) => ( {
            // Explicit clone, as into_inner consumes the pair.
        // We only need a reference to the pair later for logging.
            let $current = $token.clone()
                .into_inner()
                .filter(|rule|
                    rule.as_rule() != Rule::BLOCK_CLOSE &&
                    rule.as_rule() != Rule::BLOCK_OPEN &&
                    rule.as_rule() != Rule::WHITESPACE &&
                    rule.as_rule() != Rule::COMMENT)
                .next().unwrap();
            match $current.as_rule() {
                $(
                    $pattern => $result
                ),*
            }
        }
    );
);

// Lib exports.

pub mod ast;
pub use ast::parser;
pub use ast::renderer;
pub mod dml;
pub use dml::validator::ValidationPipeline;
pub use dml::*;
pub mod common;
pub use crate::common::FromStrAndSpan;
pub use common::argument::Arguments;
pub mod configuration;
pub mod dmmf;
pub mod errors;
pub use common::functions::FunctionalEvaluator;
pub use configuration::*;
pub use validator::directive::DirectiveValidator;

use std::io::Write;

// Convenience Helpers
pub fn get_builtin_sources() -> Vec<Box<SourceDefinition>> {
    vec![
        Box::new(configuration::builtin::MySqlSourceDefinition::new()),
        Box::new(configuration::builtin::PostgresSourceDefinition::new()),
        Box::new(configuration::builtin::SqliteSourceDefinition::new()),
    ]
}

/// Parses and validates a datamodel string, using core attributes and the given plugins.
pub fn parse_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<configuration::SourceDefinition>>,
) -> Result<Datamodel, errors::ErrorCollection> {
    let ast = parser::parse(datamodel_string)?;
    let mut source_loader = SourceLoader::new();
    for source in get_builtin_sources() {
        source_loader.add_source_definition(source);
    }
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }
    let sources = source_loader.load(&ast)?;
    let validator = ValidationPipeline::with_sources(&sources);
    validator.validate(&ast)
}

/// Loads all source configuration blocks from a datamodel using the given source definitions.
#[deprecated(note = "please use `load_configuration_with_plugins` instead")]
pub fn load_data_source_configuration_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<configuration::SourceDefinition>>,
) -> Result<Vec<Box<Source>>, errors::ErrorCollection> {
    let ast = parser::parse(datamodel_string)?;
    let mut source_loader = SourceLoader::new();
    for source in get_builtin_sources() {
        source_loader.add_source_definition(source);
    }
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }
    source_loader.load(&ast)
}

/// Loads all configuration blocks from a datamodel using the given source definitions.
pub fn load_configuration_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<configuration::SourceDefinition>>,
) -> Result<Configuration, errors::ErrorCollection> {
    let ast = parser::parse(datamodel_string)?;

    let mut source_loader = SourceLoader::new();
    for source in get_builtin_sources() {
        source_loader.add_source_definition(source);
    }
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }

    let datasources = source_loader.load(&ast)?;

    let generators = GeneratorLoader::lift(&ast)?;

    Ok(Configuration {
        datasources,
        generators,
    })
}

/// Loads all source configuration blocks from a datamodel using the built-in source definitions.
#[deprecated(note = "please use `load_configuration` instead")]
pub fn load_data_source_configuration(datamodel_string: &str) -> Result<Vec<Box<Source>>, errors::ErrorCollection> {
    #[allow(deprecated)]
    load_data_source_configuration_with_plugins(datamodel_string, vec![])
}

/// Loads all configuration blocks from a datamodel using the built-in source definitions.
pub fn load_configuration(datamodel_string: &str) -> Result<Configuration, errors::ErrorCollection> {
    load_configuration_with_plugins(datamodel_string, vec![])
}

/// Parses and validates a datamodel string, using core attributes only.
pub fn parse(datamodel_string: &str) -> Result<Datamodel, errors::ErrorCollection> {
    parse_with_plugins(datamodel_string, vec![])
}

/// Parses and validates a datamodel string, using core attributes only.
/// In case of an error, a pretty, colorful string is returned.
pub fn parse_with_formatted_error(datamodel_string: &str, file_name: &str) -> Result<Datamodel, String> {
    match parse_with_plugins(datamodel_string, vec![]) {
        Ok(dml) => Ok(dml),
        Err(errs) => {
            let mut buffer = std::io::Cursor::new(Vec::<u8>::new());

            for error in errs.to_iter() {
                writeln!(&mut buffer, "").expect("Failed to render error.");
                error
                    .pretty_print(&mut buffer, file_name, datamodel_string)
                    .expect("Failed to render error.");
            }

            Err(String::from_utf8(buffer.into_inner()).expect("Failed to convert error buffer."))
        }
    }
}

/// Parses a datamodel string to an AST. For internal use only.
pub fn parse_to_ast(datamodel_string: &str) -> Result<ast::Datamodel, errors::ErrorCollection> {
    parser::parse(datamodel_string)
}

/// Renders an datamodel AST to a stream as datamodel string. For internal use only.
pub fn render_ast_to(stream: &mut std::io::Write, datamodel: &ast::Datamodel, ident_width: usize) {
    let mut renderer = renderer::Renderer::new(stream, ident_width);
    renderer.render(datamodel);
}

/// Renders a datamodel to a stream as datamodel string.
pub fn render_to(stream: &mut std::io::Write, datamodel: &dml::Datamodel) -> Result<(), errors::ErrorCollection> {
    let lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    render_ast_to(stream, &lowered, 2);
    Ok(())
}

/// Renders a datamodel and sources to a stream as datamodel string.
#[deprecated(note = "please use `render_with_config_to` instead")]
pub fn render_with_sources_to(
    stream: &mut std::io::Write,
    datamodel: &dml::Datamodel,
    sources: &Vec<Box<Source>>,
) -> Result<(), errors::ErrorCollection> {
    let mut lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    SourceSerializer::add_sources_to_ast(sources, &mut lowered);
    render_ast_to(stream, &lowered, 2);
    Ok(())
}

/// Renders a datamodel, generators and sources to a stream as datamodel string.
pub fn render_with_config_to(
    stream: &mut std::io::Write,
    datamodel: &dml::Datamodel,
    config: Configuration,
) -> Result<(), errors::ErrorCollection> {
    let mut lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    SourceSerializer::add_sources_to_ast(&config.datasources, &mut lowered);
    GeneratorLoader::add_generators_to_ast(&config.generators, &mut lowered);
    render_ast_to(stream, &lowered, 2);
    Ok(())
}

/// Renders an datamodel AST to a datamodel string. For internal use only.
pub fn render_ast(datamodel: &ast::Datamodel) -> String {
    let mut buffer = std::io::Cursor::new(Vec::<u8>::new());
    render_ast_to(&mut buffer, datamodel, 2);
    String::from_utf8(buffer.into_inner()).unwrap()
}

/// Renders a datamodel to a datamodel string.
pub fn render(datamodel: &dml::Datamodel) -> Result<String, errors::ErrorCollection> {
    let lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    Ok(render_ast(&lowered))
}

/// Renders a datamodel and sources to a datamodel string.
#[deprecated(note = "please use `render_with_config` instead")]
pub fn render_with_sources(
    datamodel: &dml::Datamodel,
    sources: &Vec<Box<Source>>,
) -> Result<String, errors::ErrorCollection> {
    let mut lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    SourceSerializer::add_sources_to_ast(sources, &mut lowered);
    Ok(render_ast(&lowered))
}

/// Renders a datamodel, sources and generators to a datamodel string.
pub fn render_with_config(
    datamodel: &dml::Datamodel,
    config: &configuration::Configuration,
) -> Result<String, errors::ErrorCollection> {
    let mut lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    SourceSerializer::add_sources_to_ast(&config.datasources, &mut lowered);
    GeneratorLoader::add_generators_to_ast(&config.generators, &mut lowered);
    Ok(render_ast(&lowered))
}

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

// Failure enum display derivation
#[macro_use]
extern crate failure;
