use crate::{data_model, PrismaResult};
use core::{ReadQueryExecutor, WriteQueryExecutor, Executor, SchemaBuilder};
use prisma_common::config::{self, ConnectionLimit, PrismaConfig, PrismaDatabase};
use prisma_models::InternalDataModelRef;
use std::sync::Arc;

#[cfg(feature = "sql")]
use sql_connector::{database::SqlDatabase, database::Sqlite};

#[derive(DebugStub)]
pub struct PrismaContext {
    pub config: PrismaConfig,
    pub internal_data_model: InternalDataModelRef,

    #[debug_stub = "#Executor#"]
    pub executor: Executor,
}

impl PrismaContext {
    pub fn new() -> PrismaResult<Self> {
        let config = config::load().unwrap();

        // FIXME: This is a weird ugly hack - make pretty
        //        Not sure why we need to clone the Arc before assigning it. When we
        //        try to Arc::clone(..) in the struct creation below it fails
        //        with incompatble type errors!
        let (data_resolver, write_executor) = match config.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite-native" => {
                let db_name = config.db_name();
                let db_folder = config
                    .database_file
                    .trim_end_matches(&format!("{}.db", db_name))
                    .trim_end_matches("/");

                let sqlite = Sqlite::new(db_folder.to_owned(), config.limit(), false).unwrap();
                let arc = Arc::new(SqlDatabase::new(sqlite));
                (Arc::clone(&arc), arc)
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };

        let db_name = config
            .databases
            .get("default")
            .unwrap()
            .db_name()
            .expect("database was not set");

        let read_exec: ReadQueryExecutor = ReadQueryExecutor { data_resolver };
        let write_exec: WriteQueryExecutor = WriteQueryExecutor {
            db_name: db_name.clone(),
            write_executor,
        };

        let executor = Executor { read_exec, write_exec };
        let internal_data_model = data_model::load(db_name)?;
        let schema = SchemaBuilder::build(internal_data_model.clone());

        Ok(Self {
            config,
            internal_data_model,
            executor,
        })
    }
}
