use datamodel::*;
use std::fs;

extern crate clap;
use clap::{App, Arg};

fn main() {
    let matches = App::new("Prisma Datamodel v2 to DMMF")
        .version("0.1")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Converts a datamodel v2 file to the DMMF JSON representation.")
        .arg(
            Arg::with_name("INPUT")
                .help("Sets the input datamodel file to use")
                .required(true)
                .index(1),
        )
        .get_matches();

    let file_name = matches.value_of("INPUT").unwrap();
    let file = fs::read_to_string(&file_name).expect(&format!("Unable to open file {}", file_name));

    let parsed = parser::parse(&file);

    if let Err(error) = &parsed {
        error
            .pretty_print(&mut std::io::stderr().lock(), file_name, &file)
            .expect("Failed to write error to stderr");

        return;
    }

    let ast = parsed.unwrap();
    let validator = Validator::new();
    let validated = validator.validate(&ast);

    if let Err(errors) = &validated {
        for error in errors.to_iter() {
            println!("");
            error
                .pretty_print(&mut std::io::stderr().lock(), file_name, &file)
                .expect("Failed to write errors to stderr");
        }
    }

    let dml = validated.unwrap();
    let json = datamodel::dmmf::render_to_dmmf(&dml);
    println!("{}", json);
}
