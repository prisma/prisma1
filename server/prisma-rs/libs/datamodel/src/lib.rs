pub mod ast;
pub use ast::parser;
pub mod dml;
pub use dml::validator::Validator;
pub use dml::*;
pub mod common;
pub mod dmmf;
pub mod errors;
pub mod source;
pub use common::functions::*;
pub use dml::FromStrAndSpan;
pub use source::*;

pub fn parse_with_plugins(
    datamodel_string: &str,
    source_definitions: Vec<Box<source::SourceDefinition>>,
) -> Result<Schema, errors::ErrorCollection> {
    let ast = parser::parse(datamodel_string)?;
    let mut source_loader = SourceLoader::new();
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }
    let sources = source_loader.load(&ast)?;
    let validator = Validator::with_sources(&sources);
    validator.validate(&ast)
}

pub fn parse(datamodel_string: &str) -> Result<Schema, errors::ErrorCollection> {
    return parse_with_plugins(datamodel_string, vec![]);
}

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

// Failure enum display derivation
#[macro_use]
extern crate failure;
