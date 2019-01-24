#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate serde_derive;

#[macro_use]
extern crate prost_derive;

mod config;
mod protobuf;

use config::{ConnectionLimit, PrismaConfig, PrismaDatabase};
use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::NO_PARAMS;
use serde_yaml;
use std::{
    fs::File,
    env,
};
//use prost::Message;

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

    //.unwrap().unwrap() as i32
}

#[cfg(test)]
mod test {
    use crate::config::PrismaDatabase;
    use rusqlite::NO_PARAMS;

    #[test]
    fn test_basic_select() {
        let conn = super::SQLITE_POOL.get().unwrap();
        let mut stmt = conn.prepare("SELECT 1").unwrap();

        let rows = stmt.query_map(NO_PARAMS, |row| row.get(0)).unwrap();

        for val in rows {
            let value: i32 = val.unwrap();

            assert_eq!(1, value);
        }
    }

    #[test]
    fn the_config() {
        assert_eq!(Some(4466), super::CONFIG.port);

        match super::CONFIG.databases.get("default") {
            Some(PrismaDatabase::File(config)) => {
                assert_eq!("./test.db", config.file);
            }
            _ => panic!("Unsupported database"),
        }
    }
}
