#![allow(unused, unused_mut)]

use byteorder::{BigEndian, ReadBytesExt, WriteBytesExt};
use chrono::prelude::*;
use num_traits::ToPrimitive;
use num_traits::cast::FromPrimitive;
use rust_decimal::Decimal;
use serde_json;
use uuid::Uuid;

use postgres;
use postgres::rows::{Row, Rows};
use postgres::stmt::Statement;
use postgres::transaction::Transaction;
use postgres::types::{IsNull, ToSql, Type};
use postgres::{Connection, Result as PsqlResult, TlsMode};

use std::boxed::Box;
use std::cell;
use std::cell::RefCell;
use std::error::Error as StdErr;
use std::result;

use driver::DriverError;
use driver::Result;

#[derive(Debug, PartialEq)]
pub enum JdbcParameter {
    Int(MagicInt),
    String(String),
    Boolean(bool),
    Null,
    Double(MagicFloat),
    DateTime(DateTime<Utc>),
    Long(i64),
    UUID(Uuid),
}

#[derive(Serialize, Debug)]
pub enum JdbcParameterType {
    Int,
    String,
    Boolean,
    Null,
    Double,
    DateTime,
    Long,
    UUID,
    VOID,
    StringArray
}

#[derive(Serialize, Deserialize)]
pub struct MagicDateTime {
    pub year: i32,
    pub month: u32,
    pub day: u32,
    pub hour: u32,
    pub minute: u32,
    pub seconds: u32,
    pub millis: u32,
}

impl JdbcParameter {
    pub fn paramsToSql(params: Vec<&JdbcParameter>) -> Vec<&ToSql> {
        params.into_iter().map(|p| JdbcParameter::paramToSql(p)).collect()
    }

    pub fn paramToSql(param: &JdbcParameter) -> &ToSql {
        match param {
            JdbcParameter::Int(_) => param,
            JdbcParameter::Null => param,
            JdbcParameter::Double(_) => param,
            JdbcParameter::DateTime(_) => param,
            JdbcParameter::String(ref s) => s,
            JdbcParameter::Boolean(ref b) => b,
            JdbcParameter::Long(ref d) => d,
            JdbcParameter::UUID(ref uid) => uid,
        }
    }
}

#[derive(Debug, PartialEq)]
pub struct MagicFloat {
    value: f64,
    underlying: RefCell<Option<Type>>,
}

#[derive(Debug, PartialEq)]
pub struct MagicInt {
    value: i64,
    underlying: RefCell<Option<Type>>,
}

impl MagicFloat {
    fn setType(&self, ty: Type) {
        self.underlying.replace(Some(ty));
    }
}

impl MagicInt {
    fn setType(&self, ty: Type) {
        self.underlying.replace(Some(ty));
    }
}

impl ToSql for JdbcParameter {
    fn to_sql(
        &self,
        ty: &Type,
        out: &mut Vec<u8>,
    ) -> result::Result<IsNull, Box<StdErr + Sync + Send>>
        where
            Self: Sized,
    {
        match self {
            JdbcParameter::Null => Ok(IsNull::Yes),
            JdbcParameter::DateTime(ref dt) => {
                dt.to_sql(ty, out)
            }

            JdbcParameter::Int(ref magic) => {
                let val = magic.underlying.replace(None);
                match val {
                    Some(t) => {
                        match t {
                            postgres::types::INT2 => out.write_i16::<BigEndian>(magic.value as i16).unwrap(),
                            postgres::types::INT4 => out.write_i32::<BigEndian>(magic.value as i32).unwrap(),
                            postgres::types::INT8 => out.write_i64::<BigEndian>(magic.value).unwrap(),
                            x => error!("[Rust] Unhandled MagicInt OID: {}", x),
                        }
                    }
                    x => panic!("[Rust] No underlying type present for MagicInt."),
                }

                Ok(IsNull::No)
            }

            JdbcParameter::Double(ref magic) => {
                let val = magic.underlying.replace(None);
                match val {
                    Some(t) => {
                        match t {
                            postgres::types::FLOAT8 => out.write_f64::<BigEndian>(magic.value).unwrap(), // Float8
                            postgres::types::NUMERIC => {
                                Decimal::from_f64(magic.value).unwrap().to_sql(ty, out);
                            },
                            x => error!("[Rust] Unhandled MagicFloat OID: {}", x),
                        }
                    }
                    x => panic!("[Rust] No underlying type present for MagicFloat."),
                }

                Ok(IsNull::No)
            }

            x => panic!("ToSql no match {:?}", x),
        }
    }

    fn accepts(ty: &Type) -> bool
        where
            Self: Sized,
    {
        true
    }

    fn to_sql_checked(
        &self,
        ty: &Type,
        out: &mut Vec<u8>,
    ) -> result::Result<IsNull, Box<StdErr + Sync + Send>> {
        match self {
            // Todo: Cloning is inefficient. Alternative?
            JdbcParameter::Int(ref magic) => magic.setType(ty.clone()),
            JdbcParameter::Double(ref magic) => magic.setType(ty.clone()),
            _ => (),
        };

        self.to_sql(ty, out)
    }
}

pub fn toJdbcParameterList(str: &String) -> Result<Vec<Vec<JdbcParameter>>> {
    match serde_json::from_str::<serde_json::Value>(&*str) {
        Ok(serde_json::Value::Array(elements)) => elements.iter().map(toJdbcParametersInner).collect(),
        Ok(json) => Err(DriverError::GenericError(String::from(format!(
            "provided json was not an array of arrays: {}",
            json
        )))),
        Err(e) => Err(DriverError::GenericError(String::from(format!(
            "json parsing failed: {}",
            e
        )))),
    }
}

pub fn toJdbcParameters(str: &String) -> Result<Vec<JdbcParameter>> {
    let json = serde_json::from_str::<serde_json::Value>(&*str)?;
    toJdbcParametersInner(&json)
}

fn toJdbcParametersInner(json: &serde_json::Value) -> Result<Vec<JdbcParameter>> {
    match json {
        serde_json::Value::Array(elements) => {
            let x: Result<Vec<JdbcParameter>> = elements.iter().map(jsonToJdbcParameter).collect();
            x
        },

        json => Err(DriverError::GenericError(String::from(format!(
            "provided json was not an array: {}",
            json
        )))),
    }
}

fn jsonToJdbcParameter(json: &serde_json::Value) -> Result<JdbcParameter> {
    match json {
        &serde_json::Value::Object(ref map) => jsonObjectToJdbcParameter(map),
        x => Err(DriverError::GenericError(format!(
            "{} is not a valid value for a JdbcParameter",
            x
        ))),
    }
}

fn parseDiscriminator(d: &str) -> Result<JdbcParameterType> {
    match d {
        "Int" => Ok(JdbcParameterType::Int),
        "String" => Ok(JdbcParameterType::String),
        "Boolean" => Ok(JdbcParameterType::Boolean),
        "Null" => Ok(JdbcParameterType::Null),
        "Double" => Ok(JdbcParameterType::Double),
        "DateTime" => Ok(JdbcParameterType::DateTime),
        "Long" => Ok(JdbcParameterType::Long),
        "UUID" => Ok(JdbcParameterType::UUID),
        x => Err(DriverError::GenericError(format!("discriminator {} is unhandled",d)))
    }
}

fn jsonObjectToJdbcParameter(map: &serde_json::Map<String, serde_json::Value>) -> Result<JdbcParameter> {
    let discriminator = parseDiscriminator(map.get("discriminator").unwrap().as_str().unwrap())?;
    let value = map.get("value").unwrap();

    match (discriminator, value) {
        (JdbcParameterType::Int, &serde_json::Value::Number(ref n)) => Ok(JdbcParameter::Int(MagicInt {
            value: n.as_i64().unwrap(),
            underlying: RefCell::new(None),
        })),
        (JdbcParameterType::String, &serde_json::Value::String(ref s)) => Ok(JdbcParameter::String(s.to_string())),
        (JdbcParameterType::Boolean, &serde_json::Value::Bool(b)) => Ok(JdbcParameter::Boolean(b)),
        (JdbcParameterType::Null, &serde_json::Value::Null) => Ok(JdbcParameter::Null),
        (JdbcParameterType::Double, &serde_json::Value::Number(ref n)) => Ok(JdbcParameter::Double(MagicFloat {
            value: n.as_f64().unwrap(),
            underlying: RefCell::new(None),
        })),
        (JdbcParameterType::DateTime, x @ &serde_json::Value::Object(_)) => {
            let date: MagicDateTime = serde_json::from_value(x.clone())?;
            let dateTime = Utc.ymd(date.year, date.month, date.day).and_hms_milli(date.hour, date.minute, date.seconds, date.millis);
            Ok(JdbcParameter::DateTime(dateTime))
        },
        (JdbcParameterType::Long, &serde_json::Value::Number(ref n)) => Ok(JdbcParameter::Long(n.as_i64().unwrap())),
        (JdbcParameterType::UUID, &serde_json::Value::String(ref uuid)) => Ok(JdbcParameter::UUID(Uuid::parse_str(uuid)?)),
        (d, v) => Err(DriverError::GenericError(format!("Invalid combination: {:?} value {}", d, v)))
    }
}

