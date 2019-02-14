use serde_json;
use postgres;
use postgres::rows::{Row, Rows};
use postgres::stmt::Column;
use driver::Result;
use rust_decimal::Decimal;
use chrono::prelude::*;
use driver::DriverError;
use num_traits::ToPrimitive;
use jdbc_params::{JdbcParameterType, MagicDateTime};
use uuid::Uuid;
use postgres_array::Array;

#[derive(Serialize)]
pub struct ResultSet {
    columns: Vec<ResultColumn>,
    data: Vec<serde_json::Value>
}
#[derive(Serialize)]
pub struct ResultColumn {
    name: String,
    discriminator: JdbcParameterType
}

fn mapColumn(col: &Column) -> Result<ResultColumn> {
    let discriminator = match col.type_() {
        &postgres::types::BOOL => Ok(JdbcParameterType::Boolean),
        &postgres::types::INT4 => Ok(JdbcParameterType::Int),
        &postgres::types::INT8 => Ok(JdbcParameterType::Long),
        &postgres::types::NUMERIC => Ok(JdbcParameterType::Double),
        &postgres::types::VARCHAR => Ok(JdbcParameterType::String),
        &postgres::types::TEXT => Ok(JdbcParameterType::String),
        &postgres::types::BPCHAR => Ok(JdbcParameterType::String),
        &postgres::types::TIMESTAMP => Ok(JdbcParameterType::DateTime),
        &postgres::types::UUID => Ok(JdbcParameterType::UUID),
        &postgres::types::VOID => Ok(JdbcParameterType::VOID),
        &postgres::types::NAME => Ok(JdbcParameterType::String),
        &postgres::types::NAME_ARRAY => Ok(JdbcParameterType::StringArray),
        x =>  Err(DriverError::GenericError(format!(
            "Unhandled type in map column: {}",
            x
        )))
    }?;

    Ok(ResultColumn {
        name: String::from(col.name()),
        discriminator: discriminator,
    })
}

impl ResultSet {
    pub fn create(rows: Rows) -> Result<ResultSet> {
        let data: Result<Vec<serde_json::Value>> = rows.iter().map(|r| ResultSet::serializeToJson(r)).collect();
        let columns: Result<Vec<ResultColumn>> = rows.columns().iter().map(mapColumn).collect();

        Ok(ResultSet {
            columns: columns?,
            data: data?,
        })
    }

    fn serializeToJson(row: Row) -> Result<serde_json::Value> {
        let mut vec = Vec::new();
        for (i, column) in row.columns().iter().enumerate() {
            let isNull = row.get_bytes(i).is_none();
            if !isNull {
                let json_value: serde_json::Value = match column.type_() {
                    &postgres::types::BOOL => serde_json::Value::Bool(row.get(i)),
                    &postgres::types::INT4 => {
                        let value: i32 = row.get(i);
                        let number = serde_json::Number::from_f64(value as f64).unwrap();
                        serde_json::Value::Number(number)
                    }
                    &postgres::types::INT8 => {
                        let value: i64 = row.get(i);
                        let number = serde_json::Number::from_f64(value as f64).unwrap();
                        serde_json::Value::Number(number)
                    }
                    &postgres::types::VARCHAR => serde_json::Value::String(row.get(i)),
                    &postgres::types::TEXT => serde_json::Value::String(row.get(i)),
                    &postgres::types::NUMERIC => {
                        let value: Decimal = row.get(i);
                        let number = serde_json::Number::from_f64(value.to_f64().unwrap()).unwrap();
                        serde_json::Value::Number(number)
                    }
                    &postgres::types::TIMESTAMP => {
                        let value: NaiveDateTime = row.get(i);
                        let date: DateTime<Utc> = DateTime::from_utc(value, Utc);
                        let mdt = MagicDateTime {
                            year: date.year(),
                            month: date.month(),
                            day: date.day(),
                            hour: date.hour(),
                            minute: date.minute(),
                            seconds: date.second(),
                            millis: date.timestamp_subsec_millis(),
                        };

                        serde_json::to_value(mdt)?
                    }
                    &postgres::types::UUID => {
                        let uuid: Uuid = row.get(i);
                        serde_json::Value::String(uuid.to_string())
                    },
                    &postgres::types::BPCHAR => serde_json::Value::String(row.get(i)),
                    &postgres::types::VOID => {
                        serde_json::Value::Null
                    },
                    &postgres::types::NAME => serde_json::Value::String(row.get(i)),
                    &postgres::types::NAME_ARRAY => {
                        let result: Array<String> = row.get(i);
                        let mut json = vec!();
                        for aString in result {
                            json.push(serde_json::Value::String(aString));
                        }
                        serde_json::Value::Array(foo)
                    },
                    x => {
                        return Err(DriverError::GenericError(format!(
                            "Unhandled type in json serialize: {}",
                            x
                        )))
                    }
                };

                vec.push(json_value);
            } else {
                vec.push(serde_json::Value::Null);
            }
        }

        return Ok(serde_json::Value::Array(vec));
    }
}