mod schema;

use schema::*;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct DataModelMetaFormat {
    #[serde(rename = "datamodel")]
    pub data_model: DataModel,
    pub schema: DMMFSchema,
    pub mappings: Vec<DMMFMapping>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct DMMFMapping {
    model: String,
    find_one: String,
    find_many: String,
    create: String,
    update: String,
}

/// Dummy
#[derive(Debug, Serialize, Deserialize)]
struct DataModel {
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
