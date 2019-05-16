use std::boxed::Box;

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
pub struct Argument {}

// On schema construction checks:
// - field name uniqueness
// - val NameRegexp = """^[_a-zA-Z][_a-zA-Z0-9]*$""".r match
// -

#[derive(Debug)]
pub enum InputType {
  EnumType,
  InputObjectType,
  ListInputType,
  OptionInputType,
  ScalarType,
}

#[derive(Debug)]
pub enum OutputType {
  EnumType,
  ListType(Box<OutputType>),
  ObjectType(ObjectType),
  OptionType(Box<OutputType>),
  ScalarType,
}

// Possible:
// InputType(OptionType(StringType))
