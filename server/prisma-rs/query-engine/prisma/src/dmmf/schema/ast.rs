use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct DMMFSchema {
  pub queries: Vec<DMMFQuery>,
  pub mutations: Vec<DMMFMutation>,
  pub input_types: Vec<DMMFInputType>,
  pub output_types: Vec<DMMFOutputType>,
  pub enums: Vec<DMMFEnum>,
}

impl DMMFSchema {
  pub fn new() -> Self {
    Default::default()
  }
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFQuery {
  name: String,
  args: Vec<DMMFArgument>,
  output: DMMFOutput,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFMutation {
  name: String,
  args: Vec<DMMFArgument>,
  output: DMMFOutput,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFArgument {
  name: String,

  #[serde(rename = "type")]
  typ: String,

  is_required: bool,
  is_list: bool,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFOutput {
  name: String,
  is_required: bool,
  is_list: bool,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFInputType {
  name: String,
  args: Vec<DMMFArgument>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFOutputType {
  name: String,
  fields: Vec<DMMFField>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFField {
  name: String,

  #[serde(rename = "type")]
  typ: String,

  is_required: bool,
  is_list: bool,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFEnum {
  name: String,
  values: Vec<String>,
}
