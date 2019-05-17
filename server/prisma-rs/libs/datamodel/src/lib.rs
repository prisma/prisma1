pub mod ast;
pub use ast::parser;
pub mod dml;
pub use dml::validator::Validator;
pub use dml::*;
pub mod dmmf;
pub mod errors;

pub fn parse(datamodel_string: &str) -> Schema {
    let ast = parser::parse(datamodel_string).unwrap();
    let validator = Validator::new();
    validator.validate(&ast).unwrap()
}

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

// Failure enum display derivation
#[macro_use]
extern crate failure;
