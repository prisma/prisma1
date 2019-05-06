use std::env;
use std::fs;

pub mod dmmf;
pub mod ast;
use ast::parser;
pub mod dml;
use dml::validator::Validator;

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

fn main() {
    let args: Vec<String> = env::args().collect();

    if args.len() < 2 {
        println!("usage: prisma-datamodel-2-parser FILENAME");
        return;
    }

    let file = fs::read_to_string(&args[1]).expect(&format!("Unable to open file {}", args[1]));

    let ast = parser::parse(&file);

    let validator = Validator::new();

    let dml = validator.validate(&ast);

    let json = dmmf::render_to_dmmf(&dml);

    println!("{}", json);
}
