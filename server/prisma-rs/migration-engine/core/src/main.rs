use clap::{App, Arg};
use migration_core::api::RpcApi;
use std::{fs, io::Read};

fn main() {
    env_logger::init();

    let matches = App::new("Prisma Migration Engine")
        .version(env!("CARGO_PKG_VERSION"))
        .arg(
            Arg::with_name("datamodel_location")
                .short("d")
                .long("datamodel")
                .value_name("FILE")
                .help("Path to the datamodel.")
                .takes_value(true)
                .required(true),
        )
        .get_matches();

    let datamodel_location = matches.value_of("datamodel_location").unwrap();
    let mut file = fs::File::open(&datamodel_location).unwrap();

    let mut datamodel = String::new();
    file.read_to_string(&mut datamodel).unwrap();

    let api = RpcApi::new(&datamodel).unwrap();
    api.start_server()
}
