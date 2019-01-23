#[macro_use] extern crate lazy_static;
#[macro_use] extern crate serde_derive;

mod config;

use std::{
    fs::File,
};

use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use serde_yaml;
use config::{PrismaConfig, PrismaDatabase};

lazy_static! {
    pub static ref CONFIG: PrismaConfig =
        serde_yaml::from_reader(File::open("./config/prisma.yml").unwrap()).unwrap();

    pub static ref POOL: r2d2::Pool<SqliteConnectionManager> = {
        let manager = match CONFIG.databases.get("default") {
            Some(PrismaDatabase::Sqlite(ref config)) =>
                SqliteConnectionManager::file(&config.file),
            _ =>
                panic!("Database connector is not supported, use sqlite for now!")
        };

        r2d2::Pool::new(manager).unwrap()
    };
}

#[cfg(test)]
mod test {
    use rusqlite::NO_PARAMS;
    use crate::config::PrismaDatabase;

    #[test]
    fn test_basic_select() {
        let conn = super::POOL.get().unwrap();
        let mut stmt = conn.prepare("SELECT 1").unwrap();

        let rows = stmt.query_map(NO_PARAMS, |row| {
            row.get(0)
        }).unwrap();

        for val in rows {
            let value: i32 = val.unwrap();
            
            assert_eq!(1, value);
        }
    }
    
    #[test]
    fn the_config() {
        assert_eq!(4466, super::CONFIG.port);
        
        match super::CONFIG.databases.get("default") {
            Some(PrismaDatabase::Sqlite(config)) => {
                assert_eq!("./test.db", config.file);
            },
            _ => panic!("Unsupported database"),
        }
    }
}
