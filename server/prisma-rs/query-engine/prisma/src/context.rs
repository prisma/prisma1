use crate::{data_model, PrismaResult};
use core::QueryExecutor;
use prisma_common::config::{self, ConnectionLimit, PrismaConfig, PrismaDatabase};
use prisma_models::SchemaRef;
use sqlite_connector::Sqlite;
use std::sync::Arc;

#[derive(DebugStub)]
pub struct PrismaContext {
    pub config: PrismaConfig,
    pub schema: SchemaRef,

    #[debug_stub = "#QueryExecutor#"]
    pub query_executor: QueryExecutor,
}

impl PrismaContext {
    pub fn new() -> PrismaResult<Self> {
        let config = config::load().unwrap();
        let data_resolver = match config.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite-native" => {
                let db_name = config.db_name();
                let db_folder = config
                    .database_file
                    .trim_end_matches(&format!("{}.db", db_name))
                    .trim_end_matches("/");

                // FIXME: active is misused here
                let sqlite = Sqlite::new(db_folder.to_owned(), config.limit(), false).unwrap();

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

        let schema = data_model::load(db_name)?;
        Ok(Self {
            config: config,
            schema: schema,
            query_executor: query_executor,
        })
    }
}
