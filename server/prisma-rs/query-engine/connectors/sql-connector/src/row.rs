use chrono::{DateTime, Utc};
use connector::ConnectorResult;
use prisma_models::{Node, PrismaValue, TypeIdentifier};
use serde_json;
use uuid::Uuid;

#[cfg(feature = "sqlite")]
use rusqlite::{types::Type as SqliteType, Error as SqliteError, Row as SqliteRow};

/// An allocated representation of a `Row` returned from the database.
#[derive(Debug, Clone, Default)]
pub struct PrismaRow {
    pub values: Vec<PrismaValue>,
}

impl From<PrismaRow> for Node {
    fn from(row: PrismaRow) -> Node {
        Node::new(row.values)
    }
}

pub trait ToPrismaRow {
    /// Conversion from a database specific row to an allocated `PrismaRow`. To
    /// help deciding the right types, the provided `TypeIdentifier`s should map
    /// to the returned columns in the right order.
    fn to_prisma_row<'b, T>(&'b self, idents: T) -> ConnectorResult<PrismaRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>;
}

#[cfg(feature = "sqlite")]
impl<'a, 'stmt> ToPrismaRow for SqliteRow<'a, 'stmt> {
    fn to_prisma_row<'b, T>(&'b self, idents: T) -> ConnectorResult<PrismaRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>,
    {
        fn convert(row: &SqliteRow, i: usize, typid: &TypeIdentifier) -> ConnectorResult<PrismaValue> {
            let result = match typid {
                TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
                TypeIdentifier::GraphQLID => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
                TypeIdentifier::Float => row.get_checked(i).map(|val| PrismaValue::Float(val)),
                TypeIdentifier::Relation => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
                TypeIdentifier::Int => row.get_checked(i).map(|val| PrismaValue::Int(val)),
                TypeIdentifier::Boolean => row.get_checked(i).map(|val| PrismaValue::Boolean(val)),
                TypeIdentifier::Enum => row.get_checked(i).map(|val| PrismaValue::Enum(val)),
                TypeIdentifier::Json => row.get_checked(i).and_then(|val| {
                    let val: String = val;
                    serde_json::from_str(&val).map(|r| PrismaValue::Json(r)).map_err(|err| {
                        SqliteError::FromSqlConversionFailure(i as usize, SqliteType::Text, Box::new(err))
                    })
                }),
                TypeIdentifier::UUID => {
                    let result: Result<String, _> = row.get_checked(i);

                    if let Ok(val) = result {
                        let uuid = Uuid::parse_str(val.as_ref())?;

                        Ok(PrismaValue::Uuid(uuid))
                    } else {
                        result.map(|s| PrismaValue::String(s))
                    }
                }
                TypeIdentifier::DateTime => row.get_checked(i).map(|ts: i64| {
                    let nsecs = ((ts % 1000) * 1_000_000) as u32;
                    let secs = (ts / 1000) as i64;
                    let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                    let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                    PrismaValue::DateTime(datetime)
                }),
            };

            match result {
                Ok(pv) => Ok(pv),
                Err(rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null)) => Ok(PrismaValue::Null),
                Err(e) => Err(e.into()),
            }
        }

        let mut row = PrismaRow::default();

        for (i, typid) in idents.into_iter().enumerate() {
            row.values.push(convert(self, i, typid)?);
        }

        Ok(row)
    }
}
