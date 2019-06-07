mod schema;

use core::schema::{QuerySchema, QuerySchemaRenderer};
use schema::*;
use serde::Serialize;
use datamodel;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DataModelMetaFormat {
    #[serde(rename = "datamodel")]
    pub data_model: serde_json::Value,
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

pub fn render_dmmf<'a>(dml: &'a datamodel::Datamodel, query_schema: &QuerySchema) -> DataModelMetaFormat {
    let schema = DMMFQuerySchemaRenderer::render(query_schema);
    let datamodel_json = datamodel::dmmf::render_to_dmmf_value(&dml);

    DataModelMetaFormat {
        data_model: datamodel_json,
        schema,
        mappings: vec![],
    }
}
