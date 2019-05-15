use std::fs;
mod errors;
pub mod ast;
pub mod dmmf;
use ast::parser;
pub mod dml;
use dml::validator::Validator;

// Pest grammar generation on compile time.
extern crate pest;
#[macro_use]
extern crate pest_derive;

extern crate clap;
use clap::{App, Arg};

fn main() {
    let matches = App::new("Prisma Datamodel Playgroung")
        .version("0.1")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Alpha implementation of different datamodel definition grammars.")
        .arg(
            Arg::with_name("INPUT")
                .help("Sets the input datamodel file to use")
                .required(true)
                .index(1),
        )
        .get_matches();

    let file_name = matches.value_of("INPUT").unwrap();
    let file = fs::read_to_string(&file_name).expect(&format!("Unable to open file {}", file_name));

    match parser::parse(&file) {
        Ok(ast) => {
            let validator = Validator::new();

            match validator.validate(&ast) {
                Ok(dml) => {
                    let json = dmmf::render_to_dmmf(&dml);
                    println!("{}", json);
                }
                Err(errors) => {
                    for error in errors {
                        println!("");
                        println!("Error: {}", error.message);
                        println!("File: {}:", file_name);
                        println!("");
                        let line = &file[..error.span.end].matches("\n").count();
                        let text = &file[error.span.start..error.span.end];
                        println!("{} |    {}", line, text);
                        println!("");
                    }
                }
            }
        }
        Err(error) => {
            println!("");
            println!("Error while parsing, unexpected token");
            println!("File: {}:", file_name);
            println!("");
            let line = &file[..error.span.end].matches("\n").count();
            let text = file.split("\n").collect::<Vec<&str>>()[*line];
            println!("{} |    {}", line, text);
            println!("");
        }
    }
}
