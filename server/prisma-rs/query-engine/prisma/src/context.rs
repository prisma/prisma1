use crate::{data_model, exec_loader, PrismaResult};
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

        // Load internal data model and build schema
        let internal_data_model = data_model::load(db_name)?;
        let capabilities = SupportedCapabilities::empty(); // todo connector capabilities.
        let schema_builder = QuerySchemaBuilder::new(&internal_data_model, &capabilities, BuildMode::Legacy);
        let query_schema = schema_builder.build();
        let sdl = data_model::load_v11_sdl_string().ok();
        let dm = Self::load_datamodel();

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

    fn load_datamodel_v2() -> PrismaResult<datamodel::Datamodel> {
        let dml_string = data_model::load_v2_dml_string().ok()?;
        let parsed = datamodel::parse(&dml_string);

        if let Err(errors) = &parsed {
            dbg!(&errors);
            for error in errors.to_iter() {
                println!("");
                error
                    .pretty_print(&mut std::io::stderr().lock(), "from env", &dml_string)
                    .expect("Failed to write errors to stderr");
            }

            return None;
        }

        parsed.ok()
    }
}
