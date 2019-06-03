use std::fs;

extern crate clap;
use clap::{App, Arg};

fn main() {
    let matches = App::new("Prisma DMMF to Datamodel v2")
        .version("0.1")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Converts a DMMF JSON representation to datamodel v2.")
        .arg(
            Arg::with_name("INPUT")
                .help("Sets the input dmmf file to use")
                .required(true)
                .index(1),
        )
        .get_matches();

    let file_name = matches.value_of("INPUT").unwrap();
    let file = fs::read_to_string(&file_name).expect(&format!("Unable to open file {}", file_name));

    let dml = datamodel::dmmf::parse_from_dmmf(&file);

    datamodel::render_to(&mut std::io::stdout().lock(), &dml).unwrap();
}
