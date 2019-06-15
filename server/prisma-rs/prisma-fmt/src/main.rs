use datamodel;
use std::{
    fs,
    io::{self, Read},
};

extern crate clap;
use clap::{App, Arg};

fn main() {
    let matches = App::new("Prisma Datamodel v2 formatter")
        .version("0.1")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Formats a datamodel v2 file and prints the result to standard output.")
        .arg(
            Arg::with_name("input")
                .short("i")
                .long("input")
                .value_name("INPUT_FILE")
                .required(false)
                .help("Specifies the input file to use. If none is given, the input is read from stdin."),
        )
        .arg(
            Arg::with_name("output")
                .short("o")
                .long("output")
                .value_name("OUTPUT_FILE")
                .required(false)
                .help("Specifies the output file to use. If none is given, the output is written to stdout."),
        )
        .arg(
            Arg::with_name("tabwidth")
                .short("s")
                .long("tabwidth")
                .value_name("WIDTH")
                .required(false)
                .help("Specifies wich tab width to use when formaitting. Default is 2."),
        )
        .get_matches();

    let file_name = matches.value_of("input");
    let tab_width = matches
        .value_of("tabwidth")
        .unwrap_or("2")
        .parse::<usize>()
        .expect("Error while parsing tab width.");

    // TODO: This is really ugly, clean it up.
    let datamodel_string: String = if let Some(file_name) = file_name {
        fs::read_to_string(&file_name).expect(&format!("Unable to open file {}", file_name))
    } else {
        let mut buf = String::new();
        io::stdin()
            .read_to_string(&mut buf)
            .expect("Unable to read from stdin.");
        buf
    };

    let file_name = matches.value_of("output");

    if let Some(file_name) = file_name {
        let file = std::fs::File::open(file_name).expect(&format!("Unable to open file {}", file_name));
        let mut stream = std::io::BufWriter::new(file);
        datamodel::ast::reformat::Reformatter::reformat_to(&datamodel_string, &mut stream, tab_width);
    } else {
        datamodel::ast::reformat::Reformatter::reformat_to(&datamodel_string, &mut std::io::stdout().lock(), tab_width);
    }

    std::process::exit(0);

}
