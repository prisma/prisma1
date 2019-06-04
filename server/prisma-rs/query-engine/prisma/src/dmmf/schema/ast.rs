use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct DMMFSchema {
  pub queries: Vec<DMMFField>,
  pub mutations: Vec<DMMFField>,
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
pub struct DMMFField {
  pub name: String,
  pub args: Vec<DMMFArgument>,
  pub output_type: DMMFTypeInfo,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFArgument {
  pub name: String,
  pub input_type: DMMFTypeInfo,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFInputType {
  pub name: String,
  pub fields: Vec<DMMFInputField>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFOutputType {
  pub name: String,
  pub fields: Vec<DMMFField>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFInputField {
  pub name: String,
  pub input_type: DMMFTypeInfo,
}

/// Intermediate type for generic field passing during serialization.
pub enum DMMFFieldWrapper {
  Input(DMMFInputField),
  Output(DMMFField),
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFTypeInfo {
  #[serde(rename = "type")]
  pub typ: String,
  pub kind: TypeKind,
  pub is_required: bool,
  pub is_list: bool,
}

#[derive(Clone, Copy, Debug, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum TypeKind {
  Scalar,
  Object,
  Enum,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DMMFEnum {
  pub name: String,
  pub values: Vec<String>,
}
