use datamodel;
use std::fs;

extern crate clap;
use clap::{App, Arg};

fn main() {
    let matches = App::new("Prisma Datamodel v2 to DMMF")
        .version("0.1")
        .author("Emanuel Jöbstl <emanuel.joebstl@gmail.com>")
        .about("Converts a datamodel v2 file to the MCF JSON representation.")
        .arg(
            Arg::with_name("INPUT")
                .help("Sets the input datamodel file to use")
                .required(true)
                .index(1),
        )
        .get_matches();

    let file_name = matches.value_of("INPUT").unwrap();
    let file = fs::read_to_string(&file_name).expect(&format!("Unable to open file {}", file_name));

    let res = datamodel::load_configuration(&file);

    match &res {
        Err(errors) => {
            for error in errors.to_iter() {
                println!("");
                error
                    .pretty_print(&mut std::io::stderr().lock(), file_name, &file)
                    .expect("Failed to write errors to stderr");
            }
        }
        Ok(config) => {
            let json = serde_json::to_string_pretty(&datamodel::config_to_mcf_json_value(&config)).unwrap();
            println!("{}", json);
        }
    }
}
