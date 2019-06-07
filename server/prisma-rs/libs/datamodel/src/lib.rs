// Load macros first - Global Macros used for parsing.
// Macro to match all children in a parse tree
#[macro_use]
macro_rules! match_children (
    ($token:ident, $current:ident, $($pattern:pat => $result:expr),*) => (
        // Explicit clone, as into_inner consumes the pair.
        // We only need a reference to the pair later for logging.
        for $current in $token.clone().into_inner() {
            match $current.as_rule() {
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
            let $current = $token.clone().into_inner().next().unwrap();
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
pub mod dmmf;
pub mod errors;
pub mod source;
pub use common::functions::FunctionalEvaluator;
pub use source::*;
pub use validator::directive::DirectiveValidator;

// Convenience Helpers
fn get_builtin_sources() -> Vec<Box<SourceDefinition>> {
    vec![
        Box::new(source::builtin::MySqlSourceDefinition::new()),
        Box::new(source::builtin::PostgresSourceDefinition::new()),
        Box::new(source::builtin::SqliteSourceDefinition::new()),
    ]
}

/// Parses and validates a datamodel string, using core attributes and the given plugins.
pub fn parse_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<source::SourceDefinition>>,
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
pub fn load_data_source_configuration_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<source::SourceDefinition>>,
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

/// Loads all source configuration blocks from a datamodel using the built-in source definitions.
pub fn load_data_source_configuration(datamodel_string: &str) -> Result<Vec<Box<Source>>, errors::ErrorCollection> {
    load_data_source_configuration_with_plugins(datamodel_string, vec![])
}

/// Parses and validates a datamodel string, using core attributes only.
pub fn parse(datamodel_string: &str) -> Result<Datamodel, errors::ErrorCollection> {
    parse_with_plugins(datamodel_string, vec![])
}

/// Parses a datamodel string to an AST. For internal use only.
pub fn parse_to_ast(datamodel_string: &str) -> Result<ast::Datamodel, errors::ValidationError> {
    parser::parse(datamodel_string)
}

/// Renders an datamodel AST to a stream as datamodel string. For internal use only.
pub fn render_ast_to(stream: &mut std::io::Write, datamodel: &ast::Datamodel) {
    let mut renderer = renderer::Renderer::new(stream);
    renderer.render(datamodel);
}

/// Renders a datamodel to a a stream as datamodel string.
pub fn render_to(stream: &mut std::io::Write, datamodel: &dml::Datamodel) -> Result<(), errors::ErrorCollection> {
    let lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    render_ast_to(stream, &lowered);
    Ok(())
}

/// Renders an datamodel AST to a datamodel string. For internal use only.
pub fn render_ast(datamodel: &ast::Datamodel) -> String {
    let mut buffer = std::io::Cursor::new(Vec::<u8>::new());
    render_ast_to(&mut buffer, datamodel);
    String::from_utf8(buffer.into_inner()).unwrap()
}

/// Renders a datamodel to a datamodel string.
pub fn render(datamodel: &dml::Datamodel) -> Result<String, errors::ErrorCollection> {
    let lowered = dml::validator::LowerDmlToAst::new().lower(datamodel)?;
    Ok(render_ast(&lowered))
}

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

// Failure enum display derivation
#[macro_use]
extern crate failure;
