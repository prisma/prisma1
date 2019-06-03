use super::visitor::*;
use once_cell::sync::OnceCell;
use prisma_models::{OrderBy, ScalarField, SortOrder};
use std::{
  boxed::Box,
  sync::{Arc, Weak},
};

pub type ObjectTypeStrongRef = Arc<ObjectType>;
pub type ObjectTypeRef = Weak<ObjectType>;

pub type InputObjectTypeStrongRef = Arc<InputObjectType>;
pub type InputObjectTypeRef = Weak<InputObjectType>;

/// The query schema.
/// Defines which operations (query/mutations) are possible on a database,
/// based on the (internal) data model.
#[derive(Debug)]
pub struct QuerySchema {
  pub query: OutputType,
  pub mutation: OutputType,

  /// Stores all strong refs to the input object types.
  input_object_types: Vec<InputObjectTypeStrongRef>,

  /// Stores all strong refs to the output object types.
  output_object_types: Vec<ObjectTypeStrongRef>,
}

impl QuerySchema {
  pub fn new(
    query: OutputType,
    mutation: OutputType,
    input_object_types: Vec<InputObjectTypeStrongRef>,
    output_object_types: Vec<ObjectTypeStrongRef>,
  ) -> Self {
    QuerySchema {
      query,
      mutation,
      input_object_types,
      output_object_types,
    }
  }

  // WIP
  // pub fn compact(mut self) -> Self {
  //   // Check if there are empty input objects and clean up the AST is there are any.
  //   let (valid_objects, empty_input_objects) = self.input_object_types.into_iter().partition(|i| i.is_empty());
  //   self.input_object_types = valid_objects;

  //   if empty_input_objects.len() > 0 {
  //     // Walk the AST and discard any element where the weak ref upgrade fails.
  //     self.visit_output_type(&self.query);
  //     self.visit_output_type(&self.mutation);
  //   }

  //   self
  // }

  // fn visit_output_type(&self, out: &OutputType) -> VisitorOperation<OutputType> {
  //   match out {
  //     OutputType::Object(obj) => unimplemented!(),
  //     OutputType::Enum(enum_type) => unimplemented!(),
  //     OutputType::List(out) => unimplemented!(),
  //     OutputType::Opt(out) => unimplemented!(),
  //     OutputType::Scalar(s) => unimplemented!(),
  //   }
  // }

  // fn visit(&mut self, visitor: impl SchemaAstVisitor) {
  //   match visitor.visit_output_type(&self.query) {
  //     VisitorOperation::Remove => unimplemented!(),
  //     VisitorOperation::Replace(t) => unimplemented!(),
  //     VisitorOperation::None => unimplemented!(),
  //   };

  //   visitor.visit_output_type(&self.mutation);
  // }
}

#[derive(DebugStub)]
pub struct ObjectType {
  pub name: String,

  #[debug_stub = "#Fields Cell#"]
  pub fields: OnceCell<Vec<Field>>,
}

impl ObjectType {
  pub fn get_fields(&self) -> &Vec<Field> {
    self.fields.get().unwrap()
  }

  pub fn set_fields(&self, fields: Vec<Field>) {
    self.fields.set(fields).unwrap();
  }

  /// True if fields are empty, false otherwise.
  pub fn is_empty(&self) -> bool {
    self.get_fields().is_empty()
  }
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

impl InputObjectType {
  pub fn get_fields(&self) -> &Vec<InputField> {
    self.fields.get().unwrap()
  }

  pub fn set_fields(&self, fields: Vec<InputField>) {
    self.fields.set(fields).unwrap();
  }

  /// True if fields are empty, false otherwise.
  pub fn is_empty(&self) -> bool {
    self.get_fields().is_empty()
  }
}

#[derive(Debug)]
pub struct InputField {
  pub name: String,
  pub field_type: InputType,
  // pub default_value: Option<>... todo: Do we need that?
  // FromInput conversion -> Take a look at that.
}

#[derive(Debug)]
pub enum InputType {
  Enum(EnumType),
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

  pub fn scalar_enum(referencing: EnumType) -> InputType {
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
  Enum(EnumType),
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
    InputType::Enum(e)
  }
}
