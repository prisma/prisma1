#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate serde_derive;

mod config;

use config::{PrismaConfig, ConnectionLimit, PrismaDatabase};
use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use serde_yaml;
use std::fs::File;

const SQLITE: &'static str = "sqlite";

lazy_static! {
    pub static ref POOL: r2d2::Pool<SqliteConnectionManager> = {
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
    pub static ref CONFIG: PrismaConfig =
        serde_yaml::from_reader(File::open("./config/prisma.yml").unwrap()).unwrap();
}

#[cfg(test)]
mod test {
    use crate::config::PrismaDatabase;
    use rusqlite::NO_PARAMS;

    #[test]
    fn test_basic_select() {
        let conn = super::POOL.get().unwrap();
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
