mod schema;

use core::schema::{QuerySchema, QuerySchemaRenderer};
use schema::*;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DataModelMetaFormat {
    #[serde(rename = "datamodel")]
    pub data_model: DataModel,
    pub schema: DMMFSchema,
    pub mappings: Vec<DMMFMapping>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFMapping {
    model: String,
    find_one: String,
    find_many: String,
    create: String,
    update: String,
}

/// Dummy
#[derive(Debug, Serialize, Deserialize)]
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


pub fn render_dmmf(query_schema: &QuerySchema) -> DataModelMetaFormat {
    let schema = DMMFRenderer::render(query_schema);

    DataModelMetaFormat {
        data_model: DataModel::new(),
        schema,
        mappings: vec![],
    }
}