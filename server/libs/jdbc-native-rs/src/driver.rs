#![allow(unused, unused_mut)]

use postgres;
use postgres::transaction::Transaction;
use postgres::{Connection, TlsMode, Result as PsqlResult};
use std::cell::RefCell;
use postgres::rows::Row;
use postgres::types::{IsNull, ToSql, Type};
use std::error::Error as StdErr;
use chrono::prelude::*;
use std::boxed::Box;
use std::result;
use std::cell;
use byteorder::{ReadBytesExt, WriteBytesExt, BigEndian};
use serde_json;
use rust_decimal::Decimal;

#[repr(C)]
#[no_mangle]
#[allow(non_snake_case)]
pub struct PsqlConnection<'a> {
    connection: Connection,
    transaction: RefCell<Option<Transaction<'a>>>,
}

#[derive(Debug)]
pub enum DriverError {
    JsonError(serde_json::Error),
    PsqlError(postgres::Error),
    GenericError(String),
}

pub type Result<T> = result::Result<T, DriverError>;

pub fn serializeToJson(row: Row) -> Result<serde_json::Value> {
    let mut map = serde_json::Map::new();
    for (i, column) in row.columns().iter().enumerate() {
        let x: Option<postgres::Result<String>> = row.get_opt(i);
        let isNull = match x {
            Some(Err(WasNull)) => true,
            _ => false,
        };

        if !isNull {
            let json_value: serde_json::Value = match column.type_() {
                &postgres::types::BOOL => serde_json::Value::Bool(row.get(i)),
                &postgres::types::INT4 => {
                    let value: i32 = row.get(i);
                    let number = serde_json::Number::from_f64(value as f64).unwrap();
                    serde_json::Value::Number(number)
                }
                &postgres::types::VARCHAR => serde_json::Value::String(row.get(i)),
                &postgres::types::TEXT => serde_json::Value::String(row.get(i)),
                x => panic!("type {} is not supported", x),
            };
            map.insert(String::from(column.name()), json_value);
        } else {
            map.insert(String::from(column.name()), serde_json::Value::Null);
        }
    }

    return Ok(serde_json::Value::Object(map));
}

pub fn connect<'a>(url: String) -> PsqlConnection<'a> {
    let conn = Connection::connect(url, TlsMode::None).unwrap();
    return PsqlConnection {
        connection: conn,
        transaction: RefCell::new(None),
    };
}

impl<'a> Drop for PsqlConnection<'a> {
    fn drop(&mut self) {
        println!("[Rust] Dropping psql connection");
    }
}


impl From<postgres::Error> for DriverError {
    fn from(e: postgres::Error) -> Self {
        DriverError::PsqlError(e)
    }
}

impl From<serde_json::Error> for DriverError {
    fn from(e: serde_json::Error) -> Self {
        DriverError::JsonError(e)
    }
}

impl From<cell::BorrowMutError> for DriverError {
    fn from(e: cell::BorrowMutError) -> Self {
        DriverError::GenericError(e.to_string())
    }
}


impl<'a> PsqlConnection<'a> {

    pub fn queryRawParams(&self, query: String, rawParams: String) -> Result<String> {
        let params = toGcValues(&rawParams)?;
        return self.query(query, params.iter().collect());
    }

    pub fn query(&self, query: String, params: Vec<&GcValue>) -> Result<String> {
        println!("[Rust] Query received the params: {:?}", params);
        let mutRef = self.transaction.try_borrow_mut()?;
        let rows = match *mutRef {
            Some(ref t) => t.query(&*query, &gcValuesToToSql(params))?,
            None => self
                .connection
                .query(&*query, &gcValuesToToSql(params))?,
        };

        println!("[Rust] The result set has {} columns", rows.columns().len());
        for column in rows.columns() {
            println!("[Rust] column {} of type {}", column.name(), column.type_());
        }

        let mut vec = Vec::new();
        for row in rows.iter() {
            let json = serializeToJson(row)?;
            vec.push(json);
        }

        return Ok(serde_json::to_string(&vec)?);
    }

    pub fn executeRawParams(&self, query: String, rawParams: String) -> Result<u64> {
        let params = toGcValues(&rawParams)?;
        return self.execute(query, params.iter().collect());
    }

    pub fn execute(&self, query: String, params: Vec<&GcValue>) -> Result<u64> {
        println!("[Rust] Execute received the params: {:?}", params);

        let mutRef = self.transaction.try_borrow_mut()?;
        let result = match *mutRef {
            Some(ref t) => {
                println!("[Rust] Have transaction");
                t.execute(&*query, &gcValuesToToSql(params))?
            }
            None => self
                .connection
                .execute(&*query, &gcValuesToToSql(params))?,
        };


        println!("[Rust] EXEC DONE");
        return Ok(result);
    }

    pub fn close(self) {
        // Simply drops the moved var for now, which calls the drop Impl
    }

    pub fn startTransaction(&'a mut self) -> Result<()> {
        let ta = self.connection.transaction()?;
        self.transaction.replace(Some(ta));

        return Ok(());
    }

    pub fn commitTransaction(&self) -> Result<()> {
        let taOpt = self.transaction.replace(None);
        match taOpt {
            Some(ta) => {
                println!("[Rust] Have transaction");
                Ok(ta.commit()?)
            }
            None => Ok(()),
        }
    }

    pub fn rollbackTransaction(&self) -> Result<()> {
        let taOpt = self.transaction.replace(None);
        match taOpt {
            Some(ta) => {
                println!("[Rust] Have transaction");
                Ok(ta.set_rollback())
            }

            None => Ok(()),
        }
    }
}

pub fn gcValuesToToSql(values: Vec<&GcValue>) -> Vec<&ToSql> {
    values.into_iter().map(gcValueToToSql).collect()
}

fn gcValueToToSql(value: &GcValue) -> &ToSql {
    match value {
        &GcValue::Int(ref i) => i,
        &GcValue::String(ref s) => s,
        &GcValue::Boolean(ref b) => b,
        &GcValue::Null => value,
        &GcValue::Double(ref d) => value,
        &GcValue::Long(ref d) => d,
        &GcValue::DateTime(ref i) => value,
    }
}

pub fn toGcValues(str: &String) -> Result<Vec<GcValue>> {
    match serde_json::from_str::<serde_json::Value>(&*str) {
        Ok(serde_json::Value::Array(elements)) => elements.iter().map(jsonToGcValue).collect(),
        Ok(json) => Err(DriverError::GenericError(String::from(format!(
            "provided json was not an array: {}",
            json
        )))),
        Err(e) => Err(DriverError::GenericError(String::from(format!("json parsing failed: {}", e)))),
    }
}

pub fn toGcValue(str: String) -> Result<GcValue> {
    match serde_json::from_str::<serde_json::Value>(&*str) {
        Ok(result) => jsonToGcValue(&result),
        Err(e) => Err(DriverError::GenericError(String::from(format!("json parsing failed: {}", e)))),
    }
}

fn jsonToGcValue(json: &serde_json::Value) -> Result<GcValue> {
    match json {
        &serde_json::Value::Object(ref map) => jsonObjectToGcValue(map),
        x => Err(DriverError::GenericError(format!("{} is not a valid value for a GcValue", x))),
    }
}

fn jsonObjectToGcValue(map: &serde_json::Map<String, serde_json::Value>) -> Result<GcValue> {
    let discriminator = map.get("discriminator").unwrap().as_str().unwrap();
    let value = map.get("value").unwrap();

    match (discriminator, value) {
        ("Int", &serde_json::Value::Number(ref n)) => Ok(GcValue::Int(n.as_i64().unwrap() as i32)),
        ("String", &serde_json::Value::String(ref s)) => Ok(GcValue::String(s.to_string())),
        ("Boolean", &serde_json::Value::Bool(b)) => Ok(GcValue::Boolean(b)),
        ("Null", &serde_json::Value::Null) => Ok(GcValue::Null),
        ("Double", &serde_json::Value::Number(ref n)) => Ok(GcValue::Double(MagicFloat { value: n.as_f64().unwrap(), underlying: RefCell::new(None) })),
        ("DateTime", &serde_json::Value::Number(ref n)) => Ok(GcValue::DateTime(n.as_i64().unwrap())),
        ("Long", &serde_json::Value::Number(ref n)) => Ok(GcValue::Long(n.as_i64().unwrap())),
        (d, v) => Err(DriverError::GenericError(format!(
            "discriminator {} and value {} are invalid combinations",
            d, v
        ))),
    }
}

#[derive(Debug, PartialEq)]
pub enum GcValue {
    Int(i32),
    String(String),
    Boolean(bool),
    Null,
    Double(MagicFloat),
    DateTime(i64),
    Long(i64),
}

#[derive(Debug, PartialEq)]
pub struct MagicFloat {
    value: f64,
    underlying: RefCell<Option<Type>>,
}

impl MagicFloat {
    fn setType(&self, ty: Type) {
        self.underlying.replace(Some(ty));
    }
}
use num_traits::cast::FromPrimitive;

impl ToSql for GcValue {
    fn to_sql(&self, ty: &Type, out: &mut Vec<u8>) -> result::Result<IsNull, Box<StdErr + Sync + Send>> where
        Self: Sized {
        match self {
            GcValue::Null => Ok(IsNull::Yes),
            GcValue::DateTime(ref i) => Utc.timestamp(i / 1000, 0).naive_utc().to_sql(ty, out),
            GcValue::Double(ref magic) => {
                let val = magic.underlying.replace(None); // todo replace because I'm not smart enough to get it otherwise
                match val {
                    Some(ref t) => {
                        println!("[Rust] Writing MagicFloat with OID: {}", t.oid());
                        match t.oid() {
                            701 => out.write_f64::<BigEndian>(magic.value).unwrap(), // Float8
                            1700 => {
                                Decimal::from_f64(magic.value).unwrap().to_sql(ty, out); // todo unwrap
                            }, // Numeric
                            x => println!("[Rust] Unhandled MagicFloat OID: {}", t.oid()),
                        }
                    },
                    x => panic!("[Rust] No underlying type present for MagicFloat."),
                }

                Ok(IsNull::No)
            }
            x => panic!("ToSql no match {:?}", x)
        }
    }

    fn accepts(ty: &Type) -> bool where Self: Sized {
        true
    }

    fn to_sql_checked(&self, ty: &Type, out: &mut Vec<u8>) -> result::Result<IsNull, Box<StdErr + Sync + Send>> {
        match self {
            GcValue::Double(ref magic) => magic.setType(ty.clone()), // Todo: Cloning is inefficient. Alternative?
            _ => (),
        };

        self.to_sql(ty, out)
    }
}
