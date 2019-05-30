use core::schema::*;
use serde::{Deserialize, Serialize};
use std::{cell::RefCell, collections::HashMap};

struct DMMFRenderer;

impl QuerySchemaRenderer<DataModelMetaFormat> for DMMFRenderer {
    fn render(query_schema: &QuerySchema) -> DataModelMetaFormat {
        unimplemented!()
    }
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

#[derive(Debug, Serialize, Deserialize)]
struct DMMFSchema {
    queries: Vec<DMMFQuery>,
    mutations: Vec<DMMFMutation>,
    input_types: Vec<DMMFOperation>,
    output_types: Vec<DMMFOperation>,
    enums: Vec<DMMFEnum>,
}

#[derive(Debug, Serialize, Deserialize)]
struct DMMFQuery {}

#[derive(Debug, Serialize, Deserialize)]
struct DMMFMutation {}

#[derive(Debug, Serialize, Deserialize)]
struct DMMFOperation {}

#[derive(Debug, Serialize, Deserialize)]
struct DMMFEnum {}

#[derive(Debug, Serialize, Deserialize)]
struct DataModelMetaFormat {
    pub data_model: DataModel,
    pub schema: DMMFSchema,
}

pub struct RenderContext {
    /// Aggregator
    result: DataModelMetaFormat,

    /// Prevents double rendering of elements that are referenced multiple times.
    /// Names of input / output types / enums / models are globally unique.
    rendered: RefCell<HashMap<String, ()>>,
}

impl RenderContext {
    pub fn new() -> Self {
        RenderContext {
            result: DataModelMetaFormat {
                data_model: DataModel::new(), // dummy
                schema: DMMFSchema::new(),
            },
        }
    }
}
