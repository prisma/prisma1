use clap::{App, Arg};
use migration_core::{api::RpcApi, Error};
use std::{fs, io::Read, env, io};
use prisma_common::logger::Logger;

fn main() {
    let _logger = Logger::build("prisma"); // keep in scope

    let matches = App::new("Prisma Migration Engine")
        .version(env!("CARGO_PKG_VERSION"))
        .arg(
            Arg::with_name("datamodel_location")
                .short("d")
                .long("datamodel")
                .value_name("FILE")
                .help("Path to the datamodel.")
                .takes_value(true)
                .required(false),
        )
        .arg(
            Arg::with_name("single_cmd")
                .short("s")
                .long("single_cmd")
                .help("Run only a single command, then exit")
                .takes_value(false)
                .required(false),
        )
        .get_matches();

    let dml_loc = matches.value_of("datamodel_location").unwrap();
    let mut file = fs::File::open(&dml_loc).unwrap();

    let mut datamodel = String::new();
    file.read_to_string(&mut datamodel).unwrap();

    if matches.is_present("single_cmd") {
        let api = RpcApi::new_sync(&datamodel).unwrap();
        let response = api.handle().unwrap();

        println!("{}", response);
    } else {
        match RpcApi::new_async(&datamodel) {
            Ok(api) => {
                api.start_server()
            }
            Err(Error::DatamodelError(errors)) => {
                let file_name = env::var("PRISMA_SDL_PATH").unwrap_or_else(|_| "schema.prisma".to_string());

                for error in errors.to_iter() {
                    println!("");
                    error
                        .pretty_print(&mut io::stderr().lock(), &file_name, &datamodel)
                        .expect("Failed to write errors to stderr");
                }

                std::process::exit(1);
            }
            Err(e) => panic!(e)
        }

    }
}
