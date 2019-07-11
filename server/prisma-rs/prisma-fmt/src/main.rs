use datamodel;
use datamodel::errors::ValidationError;
use std::{
    fs,
    io::{self, Read},
};

use clap::{App, Arg};
use serde;
use serde_json;

#[derive(serde::Serialize)]
struct MiniError {
    start: usize,
    end: usize,
    text: String,
}

fn main() {
    let matches = App::new("Prisma Datamodel v2 formatter")
        .version("0.2")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Formats or lints a datamodel v2 file and prints the result to standard output.")
        .arg(
            Arg::with_name("input")
                .short("i")
                .long("input")
                .value_name("INPUT_FILE")
                .required(false)
                .help("Specifies the input file to use. If none is given, the input is read from stdin."),
        )
        .arg(
            Arg::with_name("lint")
                .short("l")
                .long("lint")
                .required(false)
                .help("Specifies linter mode."),
        )  
        .arg(
            Arg::with_name("no_env_errors")
                .long("no_env_errors")
                .required(false)
                .help("If set, silences all `environment variable not found` errors."),
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

    if matches.is_present("lint") {
        // Linter
        let skip_env_errors = matches.is_present("no_env_errors");
        let mut datamodel_string = String::new();
        io::stdin()
            .read_to_string(&mut datamodel_string)
            .expect("Unable to read from stdin.");

        if let Err(err) = datamodel::parse(&datamodel_string) {
            let errs: Vec<MiniError> = err
                .errors
                .iter()
                .filter(|err: &&ValidationError| match err {
                    ValidationError::EnvironmentFunctionalEvaluationError { var_name: _, span: _} => !skip_env_errors,
                    _ => true
                })
                .map(|err: &ValidationError| MiniError {
                    start: err.span().start,
                    end: err.span().end,
                    text: format!("{}", err),
                })
                .collect();
            let json = serde_json::to_string(&errs).expect("Failed to render JSON");
            print!("{}", json)
        } else {
            print!("[]");
        }

        std::process::exit(0);
    } else {
        // Formatter
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
            datamodel::ast::reformat::Reformatter::reformat_to(
                &datamodel_string,
                &mut std::io::stdout().lock(),
                tab_width,
            );
        }
        std::process::exit(0);
    }
}
