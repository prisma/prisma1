use super::*;

pub fn object_type<T>(name: T, fields: Vec<Field>) -> ObjectType
where
  T: Into<String>,
{
  ObjectType {
    name: name.into(),
    fields,
  }
}

pub fn pluralize<T>(s: T) -> String
where
  T: AsRef<str>,
{
  prisma_inflector::default().pluralize(s.as_ref())
}
