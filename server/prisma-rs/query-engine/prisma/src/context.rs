use std::sync::Arc;

use core::{BuildMode, QueryExecutor, QuerySchemaBuilder, QuerySchemaRef, SupportedCapabilities};
use prisma_models::InternalDataModelRef;

use crate::{data_model_loader::*, exec_loader, PrismaError, PrismaResult};

/// Prisma request context containing all immutable state of the process.
/// There is usually only one context initialized per process.
#[derive(DebugStub)]
pub struct PrismaContext {
    /// Internal data model used throughout the query engine.
    pub internal_data_model: InternalDataModelRef,

    /// The api query schema.
    pub query_schema: QuerySchemaRef,

    /// DML-based v2 datamodel.
    pub dm: datamodel::Datamodel,

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
        let (v2components, template) = load_data_model_components()?;

        let (dm, data_sources) = (v2components.datamodel, v2components.data_sources);

        // We only support one data source at the moment, so take the first one (default not exposed yet).
        let data_source = if data_sources.is_empty() {
            return Err(PrismaError::ConfigurationError("No valid data source found".into()));
        } else {
            data_sources.first().unwrap()
        };

        // Load executor
        let executor = exec_loader::load(&**data_source)?;
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
}
