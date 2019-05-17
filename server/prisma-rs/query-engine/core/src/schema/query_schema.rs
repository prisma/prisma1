use prisma_models::{OrderBy, ScalarField, SortOrder};
use std::{boxed::Box, sync::Arc};

#[derive(Debug)]
pub struct QuerySchema {
  pub query: ObjectType,    // read(s)?
  pub mutation: ObjectType, // write(s)?
}

#[derive(Debug)]
pub struct ObjectType {
  pub name: String,
  pub fields: Vec<Field>,
}

#[derive(Debug)]
pub struct Field {
  pub name: String,
  pub arguments: Vec<Argument>,
  pub field_type: OutputType,
}

#[derive(Debug)]
pub struct Argument {
  pub name: String,
  pub argument_type: InputType,
  // pub default_value: Option<>... todo: Do we need that?
  // FromInput conversion -> Take a look at that.
}

// On schema construction checks:
// - field name uniqueness
// - val NameRegexp = """^[_a-zA-Z][_a-zA-Z0-9]*$""".r match
// -

#[derive(Debug)]
pub enum InputType {
  Enum(EnumType),
  List(Box<InputType>),
  Object(ObjectType),
  Opt(Box<InputType>),
  Scalar(ScalarType),
}

impl InputType {
  pub fn list(containing: InputType) -> InputType {
    InputType::List(Box::new(containing))
  }

  pub fn opt(containing: InputType) -> InputType {
    InputType::Opt(Box::new(containing))
  }

  // pub fn enum(name: , )

  pub fn string() -> InputType {
    InputType::Scalar(ScalarType::String)
  }

  pub fn int() -> InputType {
    InputType::Scalar(ScalarType::Int)
  }
}

#[derive(Debug)]
pub enum OutputType {
  Enum(EnumType),
  List(Box<OutputType>),
  Object(ObjectType),
  Opt(Box<OutputType>),
  Scalar(ScalarType),
}

impl OutputType {
  pub fn list(containing: OutputType) -> OutputType {
    OutputType::List(Box::new(containing))
  }

  pub fn opt(containing: OutputType) -> OutputType {
    OutputType::Opt(Box::new(containing))
  }

  pub fn string() -> OutputType {
    OutputType::Scalar(ScalarType::String)
  }

  pub fn int() -> OutputType {
    OutputType::Scalar(ScalarType::Int)
  }
}

#[derive(Debug)]
pub enum ScalarType {
  String,
  Int,
}

#[derive(Debug)]
pub struct EnumType {
  pub name: String,
  pub values: Vec<EnumValue>,
}

/// Values in enums are solved with an enum rather than a trait or generic
/// to avoid cluttering all type defs in this file, essentially.
#[derive(Debug)]
pub struct EnumValue {
  name: String,
  value: SchemaEnumValues,
}

impl EnumValue {
  pub fn order_by<T>(name: T, field: Arc<ScalarField>, sort_order: SortOrder) -> Self
  where
    T: Into<String>,
  {
    EnumValue {
      name: name.into(),
      value: SchemaEnumValues::OrderBy(OrderBy { field, sort_order }),
    }
  }
}

#[derive(Debug)]
enum SchemaEnumValues {
  OrderBy(OrderBy),
}

impl From<EnumType> for OutputType {
  fn from(e: EnumType) -> Self {
    OutputType::Enum(e)
  }
}

impl From<EnumType> for InputType {
  fn from(e: EnumType) -> Self {
    InputType::Enum(e)
  }
}
