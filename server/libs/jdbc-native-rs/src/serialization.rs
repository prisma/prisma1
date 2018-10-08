use serde_json;
use postgres;
use postgres::rows::{Row, Rows};
use driver::Result;
use rust_decimal::Decimal;
use chrono::prelude::*;
use driver::DriverError;
use num_traits::ToPrimitive;

#[derive(Serialize)]
pub struct ResultSet {
    columns: Vec<String>,
    data: Vec<serde_json::Value>
}

impl ResultSet {
    pub fn create(rows: Rows) -> Result<ResultSet> {
        let data: Result<Vec<serde_json::Value>> = rows.iter().map(|r| ResultSet::serializeToJson(r)).collect();

        Ok(ResultSet {
            columns: rows.columns().iter().map(|c| String::from(c.name())).collect(),
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
                        let number = serde_json::Number::from_f64(value.timestamp_millis() as f64).unwrap();
                        serde_json::Value::Number(number)
                    }
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