#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate serde_derive;
#[macro_use]
extern crate prost_derive;

mod project;
mod schema;
mod config;
mod protobuf;
mod connector;
mod error;

use config::{ConnectionLimit, PrismaConfig, PrismaDatabase};
use protobuf::prisma::GetNodeByWhere;
use serde_yaml;
use prost::Message;
use connector::{Connector, Sqlite};
use error::Error;

use std::{
    fs::File,
    env,
    slice,
};

type PrismaResult<T> = Result<T, Error>;

lazy_static! {
    pub static ref SQLITE: Sqlite = {
        match CONFIG.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite" => {
                connector::Sqlite::new(&config.file, config.limit()).unwrap()
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        }
    };

    pub static ref CONFIG: PrismaConfig = {
        let root = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
        let path = format!("{}/prisma-rs/config/prisma.yml", root);

        dbg!(&path);

        serde_yaml::from_reader(File::open(path).unwrap()).unwrap()
    };
}

#[no_mangle]
pub extern "C" fn select_1() -> i32 {
    SQLITE.select_1().unwrap()
}

#[no_mangle]
pub extern "C" fn get_node_by_where(data: *mut u8, len: usize) {
    let payload = unsafe { slice::from_raw_parts_mut(data, len) };
    let params = GetNodeByWhere::decode(payload).unwrap();
    dbg!(params);
}
