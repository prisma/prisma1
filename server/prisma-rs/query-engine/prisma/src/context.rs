use crate::{data_model_loader::*, exec_loader, PrismaResult};
use core::{BuildMode, Executor, QuerySchema, QuerySchemaBuilder, SupportedCapabilities};
use prisma_common::config::{self, PrismaConfig};
use prisma_models::InternalDataModelRef;

#[derive(DebugStub)]
pub struct PrismaContext {
    pub config: PrismaConfig,
    pub internal_data_model: InternalDataModelRef,

    /// The api query schema.
    pub query_schema: QuerySchema,

    /// This is currently used as a temporary workaround.
    /// Setting this option will make the /
    pub sdl: Option<String>,

    /// DML based datamodel.
    pub dm: Option<datamodel::Datamodel>,

    #[debug_stub = "#Executor#"]
    pub executor: Executor,
}

impl PrismaContext {
    /// Initializes a new Prisma context.
    /// Loads all immutable state for the query engine:
    /// 1.   The Prisma configuration (prisma.yml) & dependent initialization like executors / connectors.
    /// 2.   The data model. This has different options on how to initialize, in descending order of precedence:
    /// 2.1. The datamodel loading is bypassed by providing a pre-build internal data model template
    ///      via PRISMA_INTERNAL_DATA_MODEL_JSON. This is only intended to be used by integration tests or in
    ///      rare cases where we don't want to compute the data model.
    /// 2.2. The v2 data model is provided either as file (PRISMA_DML_PATH) or as string in the env (PRISMA_DML).
    /// 2.3. The v1 data model is provided either as file (PRISMA_SDL_PATH) or as string in the env (PRISMA_SDL).
    /// 3.   The data model is converted to the internal data model.
    /// 4.   The api query schema is constructed from the internal data model.
    pub fn new() -> PrismaResult<Self> {
        // Load config and executors
        let config = config::load().unwrap();
        let executor = exec_loader::load(&config);

        // Find db name. This right here influences how data is queried for postgres, for example.
        // Specifically, this influences the schema part of: `database`.`schema`.`table`.
        // Other connectors do not use schema and the database key of the config will be used instead.
        let db = config.databases.get("default").unwrap();
        let db_name = db.schema().or_else(|| db.db_name()).unwrap_or_else(|| "prisma".into());

        // Load data model in order of precedence.
        let (sdl, dm, internal_data_model) = load_data_model_components(db_name)?;

        // Construct query schema
        let capabilities = SupportedCapabilities::empty(); // todo connector capabilities.
        let schema_builder = QuerySchemaBuilder::new(&internal_data_model, &capabilities, BuildMode::Legacy);
        let query_schema = schema_builder.build();

        // trace!("{}", GraphQLSchemaRenderer::render(&query_schema));

        Ok(Self {
            config,
            internal_data_model,
            query_schema,
            sdl,
            dm,
            executor,
        })
    }
}
