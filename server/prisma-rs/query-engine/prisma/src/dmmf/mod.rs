mod schema;

use core::schema::{QuerySchema, QuerySchemaRenderer};
use schema::*;
use serde::Serialize;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DataModelMetaFormat<'a> {
    #[serde(rename = "datamodel")]
    pub data_model: &'a datamodel::Datamodel,
    pub schema: DMMFSchema,
    pub mappings: Vec<DMMFMapping>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFMapping {
    model: String,
    find_one: String,
    find_many: String,
    create: String,
    update: String,
}

/// Dummy
#[derive(Debug, Serialize)]
pub struct DataModel {
    enums: Vec<()>,
    models: Vec<()>,
}

impl DataModel {
    pub fn new() -> Self {
        DataModel {
            enums: vec![],
            models: vec![],
        }
    }
}

pub fn render_dmmf<'a>(dml: &'a datamodel::Datamodel, query_schema: &QuerySchema) -> DataModelMetaFormat<'a> {
    let schema = DMMFQuerySchemaRenderer::render(query_schema);

    DataModelMetaFormat {
        data_model: dml,
        schema,
        mappings: vec![],
    }
}
