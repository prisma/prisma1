use datamodel;
use std::fs;

extern crate clap;
use clap::{App, Arg};

fn main() {
    let matches = App::new("Prisma Datamodel v2 formatter")
        .version("0.1")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Formats a datamodel v2 file and prints the result to standard output.")
        .arg(
            Arg::with_name("INPUT")
                .help("Sets the input datamodel file to use")
                .required(true)
                .index(1),
        )
        .get_matches();

    let file_name = matches.value_of("INPUT").unwrap();
    let file = fs::read_to_string(&file_name).expect(&format!("Unable to open file {}", file_name));

    let ast = datamodel::parse_to_ast(&file);

    match &ast {
        Err(error) => {
            error
                .pretty_print(&mut std::io::stderr().lock(), file_name, &file)
                .expect("Failed to write errors to stderr");
        }
        Ok(ast) => {
            let rendered = datamodel::render_ast(&ast);
            println!("{}", rendered);
        }
    }
}
