use crate::{data_model_loader::*, exec_loader, PrismaError, PrismaResult};
use core::{BuildMode, QueryExecutor, QuerySchemaBuilder, QuerySchemaRef, SupportedCapabilities};
use datamodel::{Datamodel, Source};
use prisma_common::{config::load as load_config, error::CommonError};
use prisma_models::InternalDataModelRef;
use std::{convert::TryInto, sync::Arc};

/// Prisma request context containing all immutable state of the process.
/// There is usually only one context initialized per process.
#[derive(DebugStub)]
pub struct PrismaContext {
    /// Internal data model used throughout the query engine.
    pub internal_data_model: InternalDataModelRef,

    /// The api query schema.
    pub query_schema: QuerySchemaRef,

    /// DML-based v2 datamodel.
    /// Setting this option will make the /dmmf route available.
    pub dm: Option<datamodel::Datamodel>,

    /// Central query executor.
    #[debug_stub = "#QueryExecutor#"]
    pub executor: QueryExecutor,
}

impl PrismaContext {
    /// Initializes a new Prisma context.
    /// Loads all immutable state for the query engine:
    /// 1. The data model. This has different options on how to initialize. See data_model_loader module. The Prisma configuration (prisma.yml) is used as fallback.
    /// 2. The data model is converted to the internal data model.
    /// 3. The api query schema is constructed from the internal data model.
    pub fn new(legacy: bool) -> PrismaResult<Self> {
        // Load data model in order of precedence.
        let (_, v2components, template) = load_data_model_components()?;

        // Deconstruct v2 components if present, and fall back to loading the legacy config
        // to get data sources for connector initialization if no v2 data model was loaded.
        let v2: PrismaResult<(Option<Datamodel>, Vec<Box<dyn Source>>)> = match v2components {
            Some(v2) => Ok((Some(v2.datamodel), v2.data_sources)),
            None => {
                let data_sources = Self::data_sources_from_config()?;
                Ok((None, data_sources))
            }
        };

        let (dm, data_sources) = v2?;

        // We only support one data source at the moment, so take the first one (default not exposed yet).
        let data_source = if data_sources.is_empty() {
            return Err(PrismaError::ConfigurationError("No valid data source found".into()));
        } else {
            data_sources.first().unwrap()
        };

        // Load executor
        let executor = exec_loader::load(data_source)?;
        let db_name: String = executor.db_name();

        // Build internal data model
        let internal_data_model = template.build(db_name);

        // Construct query schema
        let build_mode = if legacy { BuildMode::Legacy } else { BuildMode::Modern };
        let capabilities = SupportedCapabilities::empty(); // todo connector capabilities.
        let schema_builder = QuerySchemaBuilder::new(&internal_data_model, &capabilities, build_mode);
        let query_schema: QuerySchemaRef = Arc::new(schema_builder.build());

        Ok(Self {
            internal_data_model,
            query_schema,
            dm,
            executor,
        })
    }

    /// Fallback function for legacy config, constructs data sources.
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

        config.try_into().map_err(|err: CommonError| err.into())
    }
}
