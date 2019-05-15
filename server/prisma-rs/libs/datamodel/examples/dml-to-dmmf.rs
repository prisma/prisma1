use datamodel::*;
use std::fs;

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

    // TODO: Cleanup the match.
    match parser::parse(&file) {
        Ok(ast) => {
            let validator = Validator::new();

            match validator.validate(&ast) {
                Ok(dml) => {
                    let json = datamodel::dmmf::render_to_dmmf(&dml);
                    println!("{}", json);
                }
                Err(errors) => {
                    for error in errors.to_iter() {
                        println!("");
                        datamodel::errors::pretty_print_error(file_name, &file, &error.span(), &format!("{}", error));
                    }
                }
            }
        }
        Err(error) => {
            datamodel::errors::pretty_print_error(file_name, &file, &error.span, &format!("{}", error));       
        }
    }
}
