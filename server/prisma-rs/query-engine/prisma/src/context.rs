use crate::{data_model_loader::*, exec_loader, PrismaError, PrismaResult};
use core::{BuildMode, Executor, QuerySchema, QuerySchemaBuilder, SupportedCapabilities};
use datamodel::{Datamodel, Source};
use prisma_common::config::load as load_config;
use prisma_models::InternalDataModelRef;

/// Prisma request context containing all immutable state of the process.
/// There is usually only one context initialized per process.
#[derive(DebugStub)]
pub struct PrismaContext {
    /// Internal data model used throughout the query engine.
    pub internal_data_model: InternalDataModelRef,

    /// The api query schema.
    pub query_schema: QuerySchema,

    /// Prisma SDL (data model v1). Required for rendering playground.
    /// Setting this option will make the /datamodel route available.
    pub sdl: Option<String>,

    /// DML-based v2 datamodel.
    /// Setting this option will make the /dmmf route available.
    pub dm: Option<datamodel::Datamodel>,

    /// Central executor for read and write queries.
    #[debug_stub = "#Executor#"]
    pub executor: Executor,
}

impl PrismaContext {
    /// Initializes a new Prisma context.
    /// Loads all immutable state for the query engine:
    /// 1. The Prisma configuration (prisma.yml) & dependent initialization like executors / connectors.
    /// 2. The data model. This has different options on how to initialize. See data_model_loader module.
    /// 3. The data model is converted to the internal data model.
    /// 4. The api query schema is constructed from the internal data model.
    pub fn new() -> PrismaResult<Self> {
        // Load data model in order of precedence.
        let (sdl, v2components, template) = load_data_model_components()?;

        // Deconstruct v2 components if present, and fall back to loading the legacy config
        // to get data sources for connector initialization if no v2 data model was loaded.
        let (dm, data_sources): (Option<Datamodel>, Vec<Box<dyn Source>>) = v2components
            .map(|v2| Ok((Some(v2.datamodel), v2.data_sources)))
            .unwrap_or_else(|| {
                let data_sources = Self::data_sources_from_config()?;
                Ok((None, data_sources))
            })?;

        // Load executors (connector)
        let executor = exec_loader::load(&data_sources);

        // Find DB name WIP
        let db_name: String = "".into();

        // Build internal data model
        let internal_data_model = template.build(db_name);

        // Construct query schema
        let capabilities = SupportedCapabilities::empty(); // todo connector capabilities.
        let schema_builder = QuerySchemaBuilder::new(&internal_data_model, &capabilities, BuildMode::Legacy);
        let query_schema = schema_builder.build();

        // trace!("{}", GraphQLSchemaRenderer::render(&query_schema));

        Ok(Self {
            internal_data_model,
            query_schema,
            sdl,
            dm,
            executor,
        })
    }

    /// Fallback function for legacy config.
    fn data_sources_from_config() -> PrismaResult<Vec<Box<dyn Source>>> {
        let config = load_config().map_err(|_| {
            // Fallback failed
            PrismaError::ConfigurationError(
                "Unable to load Prisma configuration from any source.
                 If Prisma was initialized using data model v1,
                 make sure to provide PRISMA_CONFIG or PRISMA_CONFIG_PATH."
                    .into(),
            )
        })?;

        unimplemented!()
    }

    // Find db name. This right here influences how data is queried for postgres, for example.
    // Specifically, this influences the schema part of: `database`.`schema`.`table`.
    // Other connectors do not use schema and the database key of the config will be used instead.
    // let db_name = db.schema().or_else(|| db.db_name()).unwrap_or_else(|| "prisma".into());

    // fn db_name()
}
