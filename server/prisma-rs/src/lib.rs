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

use config::{ConnectionLimit, PrismaConfig, PrismaDatabase};
use protobuf::prisma::GetNodeByWhere;
use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::NO_PARAMS;
use serde_yaml;
use prost::Message;

use std::{
    fs::File,
    env,
    slice,
};

const SQLITE: &'static str = "sqlite";

lazy_static! {
    pub static ref SQLITE_POOL: r2d2::Pool<SqliteConnectionManager> = {
        match CONFIG.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == SQLITE => {
                r2d2::Pool::builder()
                    .max_size(config.limit())
                    .build(SqliteConnectionManager::file(&config.file))
                    .unwrap()
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
    let conn = SQLITE_POOL.get().unwrap();
    let mut stmt = conn.prepare("SELECT 1").unwrap();
    let mut rows = stmt.query_map(NO_PARAMS, |row| row.get(0)).unwrap();

    match rows.next() {
        Some(r) => r.unwrap(),
        None => panic!("No result"),
    }
}

#[no_mangle]
pub extern "C" fn get_node_by_where(data: *mut u8, len: usize) {
    let payload = unsafe { slice::from_raw_parts_mut(data, len) };
    let params: GetNodeByWhere = GetNodeByWhere::decode(payload).unwrap();
    // dbg!(params);

    let conn = SQLITE_POOL.get().unwrap();
    dbg!(conn.execute("SELECT 1", NO_PARAMS).unwrap());
}
