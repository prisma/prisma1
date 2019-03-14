use crate::schema;
use connector::*;
use core::QueryExecutor;
use prisma_common::config::{self, ConnectionLimit, PrismaConfig, PrismaDatabase, WithMigrations};
use prisma_models::SchemaRef;
use sqlite_connector::{SqlResolver, Sqlite};

pub struct PrismaContext {
    pub config: PrismaConfig,
    pub schema: SchemaRef,
    pub query_executor: QueryExecutor,
}

impl PrismaContext {
    pub fn new() -> Self {
        let config = config::load().unwrap();
        let data_resolver = match config.databases.get("default") {
            Some(PrismaDatabase::Explicit(ref config)) if config.connector == "sqlite-native" => {
                SqlResolver::new(Sqlite::new(config.limit(), config.is_active().unwrap()).unwrap())
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };
        let query_executor: QueryExecutor = QueryExecutor { Box::new(data_resolver) };
        Self {
            config: config::load().unwrap(),
            schema: schema::load_schema().unwrap(),
            query_executor: query_executor,
        }
    }
}
