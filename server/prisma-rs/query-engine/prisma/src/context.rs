use crate::{data_model, exec_loader, PrismaResult};
use core::{Executor, SchemaBuilder};
use prisma_common::config::{self, PrismaConfig};
use prisma_models::InternalDataModelRef;

#[derive(DebugStub)]
pub struct PrismaContext {
    pub config: PrismaConfig,
    pub internal_data_model: InternalDataModelRef,

    #[debug_stub = "#Executor#"]
    pub executor: Executor,
}

impl PrismaContext {
    pub fn new() -> PrismaResult<Self> {
        // Load config and executors
        let config = config::load().unwrap();
        let executor = exec_loader::load(&config);

        // Find db name. This right here influences how
        let db = config.databases.get("default").unwrap();
        let db_name = db.schema().or_else(|| db.db_name()).unwrap_or_else(|| "prisma".into());

        // Load internal data model
        let internal_data_model = data_model::load(db_name)?;
        let _ = SchemaBuilder::build(internal_data_model.clone());

        Ok(Self {
            config,
            internal_data_model,
            executor,
        })
    }
}
