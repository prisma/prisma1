use rusqlite::{
    types::{Null, ToSql, ToSqlOutput},
    Error as RusqlError,
};

pub enum PrismaValue {
    String(String),
    Float(f64),
    Boolean(bool),
    Null,
    Uuid(String),
}

impl ToSql for PrismaValue {
    fn to_sql(&self) -> Result<ToSqlOutput, RusqlError> {
        let value = match self {
            PrismaValue::String(value) => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Float(value) => ToSqlOutput::from(*value),
            PrismaValue::Boolean(value) => ToSqlOutput::from(*value),
            PrismaValue::Null => ToSqlOutput::from(Null),
            PrismaValue::Uuid(value) => ToSqlOutput::from(value.as_ref() as &str),
        };

        Ok(value)
    }
}

pub struct NodeSelector<'a> {
    pub table: String,
    pub field: &'a str,
    pub value: &'a PrismaValue,
}

impl<'a> NodeSelector<'a> {
    pub fn new(
        database: &'a str,
        table: String,
        field: &'a str,
        value: &'a PrismaValue,
    ) -> NodeSelector<'a> {
        let table = format!("{}.{}", database, table);

        NodeSelector {
            table,
            field,
            value,
        }
    }
}
