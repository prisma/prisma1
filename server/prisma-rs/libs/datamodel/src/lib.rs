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
pub use dml::validator::Validator;
pub use dml::*;
pub mod common;
pub use common::argument::Arguments;
pub mod dmmf;
pub mod errors;
pub mod source;
pub use common::functions::FunctionalEvaluator;
pub use dml::FromStrAndSpan;
pub use source::*;
pub use validator::directive::DirectiveValidator;

// Helpers.

pub fn parse_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<source::SourceDefinition>>,
) -> Result<Datamodel, errors::ErrorCollection> {
    let ast = parser::parse(datamodel_string)?;
    let mut source_loader = SourceLoader::new();
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }
    let sources = source_loader.load(&ast)?;
    let validator = Validator::with_sources(&sources);
    validator.validate(&ast)
}

pub fn parse(datamodel_string: &str) -> Result<Datamodel, errors::ErrorCollection> {
    return parse_with_plugins(datamodel_string, vec![]);
}

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

// Failure enum display derivation
#[macro_use]
extern crate failure;
