use crate::{data_model, PrismaResult};
use core::{ReadQueryExecutor, SchemaBuilder};
use prisma_common::config::{self, ConnectionLimit, PrismaConfig, PrismaDatabase};
use prisma_models::SchemaRef;
use std::sync::Arc;

#[cfg(feature = "sql")]
use sql_connector::{database::SqlDatabase, database::Sqlite};

#[derive(DebugStub)]
pub struct PrismaContext {
    pub config: PrismaConfig,
    pub schema: SchemaRef,

    #[debug_stub = "#QueryExecutor#"]
    pub read_query_executor: ReadQueryExecutor,
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

                let sqlite = Sqlite::new(db_folder.to_owned(), config.limit(), false).unwrap();
                Arc::new(SqlDatabase::new(sqlite))
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };

        let read_query_executor: ReadQueryExecutor = ReadQueryExecutor { data_resolver };

        let db_name = config
            .databases
            .get("default")
            .unwrap()
            .db_name()
            .expect("database was not set");

        // WIP TODO get naming straight!
        let data_model = data_model::load(db_name)?;

        let schema = SchemaBuilder::build(data_model.clone());

        Ok(Self {
            config: config,
            schema: data_model,
            read_query_executor,
        })
    }
}
