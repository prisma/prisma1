use super::*;
use prisma_models::{ModelRef, SortOrder};
use std::sync::Arc;

/// Object type convenience wrapper function.
pub fn object_type<T>(name: T, fields_fn: FieldsFn) -> ObjectType
where
  T: Into<String>,
{
  ObjectType {
    name: name.into(),
    fields_fn,
  }
}

/// Enum type convenience wrapper function.
pub fn enum_type<T>(name: T, values: Vec<EnumValue>) -> EnumType
where
  T: Into<String>,
{
  EnumType {
    name: name.into(),
    values: values,
  }
}

/// Argument convenience wrapper function.
pub fn argument<T>(name: T, arg_type: InputType) -> Argument
where
  T: Into<String>,
{
  Argument {
    name: name.into(),
    argument_type: arg_type,
  }
}

/// Field convenience wrapper function.
pub fn field<T>(name: T, arguments: Vec<Argument>, field_type: OutputType) -> Field
where
  T: Into<String>,
{
  Field {
    name: name.into(),
    arguments,
    field_type,
  }
}

/// Pluralizes given (English) input string. Falls back to appending "s".
pub fn pluralize<T>(s: T) -> String
where
  T: AsRef<str>,
{
  prisma_inflector::default().pluralize(s.as_ref())
}

/// Lowercases first letter, essentially.
pub fn camel_case<T>(s: T) -> String
where
  T: Into<String>,
{
  let s = s.into();

  // This is safe to unwrap because of the validation regex for model / field
  // names used in the data model, which guarantees ASCII.
  let first_char = s.chars().next().unwrap();

  format!("{}{}", first_char.to_lowercase(), s[1..].to_owned())
}
