use crate::SqlResult;
use prisma_models::{GraphqlId, PrismaValue, Record, TypeIdentifier};
use prisma_query::ast::DatabaseValue;
use uuid::Uuid;

/// An allocated representation of a `Row` returned from the database.
#[derive(Debug, Clone, Default)]
pub struct SqlRow {
    pub values: Vec<PrismaValue>,
}

impl From<SqlRow> for Record {
    fn from(row: SqlRow) -> Record {
        Record::new(row.values)
    }
}

pub trait ToSqlRow {
    /// Conversion from a database specific row to an allocated `SqlRow`. To
    /// help deciding the right types, the provided `TypeIdentifier`s should map
    /// to the returned columns in the right order.
    fn to_sql_row<'b, T>(&'b self, idents: T) -> SqlResult<SqlRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>;
}

#[derive(Debug, PartialEq, Eq, Hash, Clone)]
pub enum SqlId {
    String(String),
    Int(usize),
    UUID(Uuid),
}

impl From<SqlId> for GraphqlId {
    fn from(sql_id: SqlId) -> Self {
        match sql_id {
            SqlId::String(s) => GraphqlId::String(s),
            SqlId::Int(i) => GraphqlId::Int(i),
            SqlId::UUID(u) => GraphqlId::UUID(u),
        }
    }
}

impl From<SqlId> for DatabaseValue {
    fn from(id: SqlId) -> DatabaseValue {
        match id {
            SqlId::String(s) => s.into(),
            SqlId::Int(i) => (i as i64).into(),
            SqlId::UUID(u) => u.into(),
        }
    }
}

impl From<&SqlId> for DatabaseValue {
    fn from(id: &SqlId) -> DatabaseValue {
        id.clone().into()
    }
}
