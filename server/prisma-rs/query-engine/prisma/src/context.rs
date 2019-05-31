use crate::{data_model, exec_loader, PrismaResult};
use core::{BuildMode, Executor, QuerySchema, QuerySchemaBuilder, SupportedCapabilities};
use prisma_common::config::{self, PrismaConfig};
use prisma_models::InternalDataModelRef;

#[derive(DebugStub)]
pub struct PrismaContext {
    pub config: PrismaConfig,
    pub internal_data_model: InternalDataModelRef,

    /// This is used later to render the SDL
    pub query_schema: QuerySchema,

    /// This is currently used, as a temporary workaround.
    pub sdl: Option<String>,

    #[debug_stub = "#Executor#"]
    pub executor: Executor,
}

impl PrismaContext {
    pub fn new() -> PrismaResult<Self> {
        // Load config and executors
        let config = config::load().unwrap();
        let executor = exec_loader::load(&config);

        // Find db name. This right here influences how data is queried for postgres, for example.
        // Specifically, this influences the schema part of: `database`.`schema`.`table`.
        // Other connectors do not use schema and the database key of the config will be used instead.
        let db = config.databases.get("default").unwrap();
        let db_name = db.schema().or_else(|| db.db_name()).unwrap_or_else(|| "prisma".into());

        // Load internal data model and build schema
        let internal_data_model = data_model::load(db_name)?;
        let capabilities = SupportedCapabilities::empty(); // todo connector capabilities.
        let schema_builder = QuerySchemaBuilder::new(&internal_data_model, &capabilities, BuildMode::Legacy);
        let query_schema = schema_builder.build();
        let sdl = data_model::load_v2_dml_string().ok(); // temporary

        // trace!("{}", GraphQLSchemaRenderer::render(&query_schema));

        Ok(Self {
            config,
            internal_data_model,
            query_schema,
            sdl,
            executor,
        })
    }
}
