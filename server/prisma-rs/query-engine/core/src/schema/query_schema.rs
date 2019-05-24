use once_cell::sync::OnceCell;
use prisma_models::{OrderBy, ScalarField, SortOrder};
use std::{
  boxed::Box,
  fmt::{self, Display},
  sync::Arc,
};

#[derive(Debug)]
pub struct QuerySchema {
  pub query: OutputType,
  pub mutation: OutputType,
}

/// Fields evaluation function.
// pub type FieldsFn = Box<FnOnce() -> Vec<Field> + Send + Sync + 'static>;

#[derive(DebugStub)]
pub struct ObjectType {
  pub name: String,

  #[debug_stub = "#Fields Cell#"]
  pub fields: OnceCell<Vec<Field>>,
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

#[derive(DebugStub)]
pub struct InputObjectType {
  pub name: String,

  #[debug_stub = "#Input Fields Cell#"]
  pub fields: OnceCell<Vec<InputField>>,
}

#[derive(Debug)]
pub struct InputField {
  pub name: String,
  pub field_type: InputType,
  // pub default_value: Option<>... todo: Do we need that?
  // FromInput conversion -> Take a look at that.
}

// On schema construction checks:
// - field name uniqueness
// - val NameRegexp = """^[_a-zA-Z][_a-zA-Z0-9]*$""".r match
// -

#[derive(Debug)]
pub enum InputType {
  Enum(EnumTypeRef),
  List(Box<InputType>),
  Object(InputObjectTypeRef),
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

  pub fn object(containing: InputObjectTypeRef) -> InputType {
    InputType::Object(containing)
  }

  pub fn string() -> InputType {
    InputType::Scalar(ScalarType::String)
  }

  pub fn int() -> InputType {
    InputType::Scalar(ScalarType::Int)
  }

  pub fn float() -> InputType {
    InputType::Scalar(ScalarType::Float)
  }

  pub fn boolean() -> InputType {
    InputType::Scalar(ScalarType::Boolean)
  }

  pub fn scalar_enum(referencing: EnumTypeRef) -> InputType {
    InputType::Scalar(ScalarType::Enum(referencing))
  }

  pub fn date_time() -> InputType {
    InputType::Scalar(ScalarType::DateTime)
  }

  pub fn json() -> InputType {
    InputType::Scalar(ScalarType::Json)
  }

  pub fn uuid() -> InputType {
    InputType::Scalar(ScalarType::UUID)
  }

  pub fn id() -> InputType {
    InputType::Scalar(ScalarType::ID)
  }
}

pub type ObjectTypeRef = Arc<ObjectType>;
pub type InputObjectTypeRef = Arc<InputObjectType>;
pub type OutputTypeRef = Arc<OutputType>;
pub type InputTypeRef = Arc<InputType>;
pub type EnumTypeRef = Arc<EnumType>;

#[derive(Debug)]
pub enum OutputType {
  Enum(EnumType),
  List(Box<OutputType>),
  Object(ObjectTypeRef),
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

  pub fn object(containing: ObjectTypeRef) -> OutputType {
    OutputType::Object(containing)
  }

  pub fn string() -> OutputType {
    OutputType::Scalar(ScalarType::String)
  }

  pub fn int() -> OutputType {
    OutputType::Scalar(ScalarType::Int)
  }

  pub fn float() -> OutputType {
    OutputType::Scalar(ScalarType::Float)
  }

  pub fn boolean() -> OutputType {
    OutputType::Scalar(ScalarType::Boolean)
  }

  pub fn date_time() -> OutputType {
    OutputType::Scalar(ScalarType::DateTime)
  }

  pub fn json() -> OutputType {
    OutputType::Scalar(ScalarType::Json)
  }

  pub fn uuid() -> OutputType {
    OutputType::Scalar(ScalarType::UUID)
  }

  pub fn id() -> OutputType {
    OutputType::Scalar(ScalarType::ID)
  }
}

#[derive(Debug)]
pub enum ScalarType {
  String,
  Int,
  Float,
  Boolean,
  Enum(EnumTypeRef),
  DateTime,
  Json,
  UUID,
  ID,
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
  pub name: String,
  pub value: EnumValueWrapper,
}

impl EnumValue {
  pub fn order_by<T>(name: T, field: Arc<ScalarField>, sort_order: SortOrder) -> Self
  where
    T: Into<String>,
  {
    EnumValue {
      name: name.into(),
      value: EnumValueWrapper::OrderBy(OrderBy { field, sort_order }),
    }
  }

  pub fn string<T>(name: T, value: String) -> Self
  where
    T: Into<String>,
  {
    EnumValue {
      name: name.into(),
      value: EnumValueWrapper::String(value),
    }
  }
}

#[derive(Debug)]
pub enum EnumValueWrapper {
  OrderBy(OrderBy),
  String(String),
}

impl From<EnumType> for OutputType {
  fn from(e: EnumType) -> Self {
    OutputType::Enum(e)
  }
}

impl From<EnumType> for InputType {
  fn from(e: EnumType) -> Self {
    InputType::Enum(Arc::new(e))
  }
}
