use crate::{schema, PrismaResult};
use core::QueryExecutor;
use prisma_common::config::{self, ConnectionLimit, PrismaConfig, PrismaDatabase};
use prisma_models::SchemaRef;
use sqlite_connector::Sqlite;
use std::sync::Arc;

pub struct PrismaContext {
    pub config: PrismaConfig,
    pub schema: SchemaRef,
    pub query_executor: QueryExecutor,
}

impl PrismaContext {
    pub fn new() -> PrismaResult<Self> {
        let config = config::load().unwrap();
        let data_resolver = match config.databases.get("default") {
            Some(PrismaDatabase::Explicit(ref config)) if config.connector == "sqlite-native" => {
                let test_mode = false;
                let sqlite = Sqlite::new(config.limit(), test_mode).unwrap();
                Arc::new(sqlite)
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };

        let query_executor: QueryExecutor = QueryExecutor { data_resolver };

        let db_name = config
            .databases
            .get("default")
            .unwrap()
            .db_name()
            .expect("database was not set");

        let schema = schema::load_schema(db_name)?;
        Ok(Self {
            config: config,
            schema: schema,
            query_executor: query_executor,
        })
    }
}
