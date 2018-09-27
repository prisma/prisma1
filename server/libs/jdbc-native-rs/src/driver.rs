#![allow(unused, unused_mut)]

extern crate postgres;
extern crate serde;
extern crate serde_json;
extern crate chrono;

use self::postgres::transaction::Transaction;
use self::postgres::{Connection, TlsMode};
use std::cell::RefCell;
use self::postgres::rows::Row;
use self::postgres::types::{IsNull, ToSql, Type};
use std::error::Error as stdErr;
use self::chrono::prelude::*;
use std::boxed::Box;

#[repr(C)]
#[no_mangle]
#[allow(non_snake_case)]
pub struct PsqlConnection<'a> {
    connection: Connection,
    transaction: RefCell<Option<Transaction<'a>>>,
}

pub fn serializeToJson(row: Row) -> serde_json::Value {
    let mut map = serde_json::Map::new();
    for (i, column) in row.columns().iter().enumerate() {
        let x: Option<Result<String, _>> = row.get_opt(i);
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

    return serde_json::Value::Object(map);
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

impl<'a> PsqlConnection<'a> {
    pub fn query(&self, query: String, params: Vec<&GcValue>) -> String {
        println!("Query received the params: {:?}", params);
        let mutRef = self.transaction.borrow_mut();
        let rows = match *mutRef {
            Some(ref t) => t.query(&*query, &gcValuesToToSql(params)).unwrap(),
            None => self
                .connection
                .query(&*query, &gcValuesToToSql(params))
                .unwrap(),
        };

        println!("The result set has {} columns", rows.columns().len());
        for column in rows.columns() {
            println!("column {} of type {}", column.name(), column.type_());
        }

        let mut vec = Vec::new();
        for row in rows.iter() {
            let json = serializeToJson(row);
            vec.push(json);
        }
        return serde_json::to_string(&vec).unwrap();
    }

    pub fn execute(&self, query: String, params: Vec<&GcValue>) {
        println!("Execute received the params: {:?}", params);

        let mutRef = self.transaction.borrow_mut();
        match *mutRef {
            Some(ref t) => {
                println!("[Rust] Have transaction");
                t.execute(&*query, &gcValuesToToSql(params)).unwrap()
            }
            None => self
                .connection
                .execute(&*query, &gcValuesToToSql(params))
                .unwrap(),
        };

        println!("EXEC DONE")
    }

    pub fn close(self) {
        // self.connection.finish().unwrap();
    }

    pub fn startTransaction(&'a mut self) {
        let ta = self.connection.transaction().unwrap();
        self.transaction.replace(Some(ta));
    }

    pub fn commitTransaction(&self) {
        // let ta = self.transaction.unwrap();
        // ta.set_commit();
        // ta.finish().unwrap();
        // self.transaction = None;

        let taOpt = self.transaction.replace(None);
        match taOpt {
            Some(ta) => {
                println!("[Rust] Have transaction");
                ta.commit().unwrap();
            }
            None => (),
        }
    }

    pub fn rollbackTransaction(&self) {
        let taOpt = self.transaction.replace(None);
        match taOpt {
            Some(ta) => {
                println!("[Rust] Have transaction");
                ta.set_rollback();
                //ta.finish().unwrap();
            }

            None => (),
        }
    }
}

pub fn gcValuesToToSql(values: Vec<&GcValue>) -> Vec<&ToSql> {
    values.into_iter().map(gcValueToToSql).collect()
}

fn gcValueToToSql(value: &GcValue) -> &ToSql {
    match value {
        &GcValue::Int(ref i) => i,
        &GcValue::String(ref str) => str,
        &GcValue::Boolean(ref b) => b,
        &GcValue::Null => value,
        &GcValue::Double(ref d) => d,
        &GcValue::Long(ref d) => d,
        &GcValue::DateTime(ref i) => value,
    }
}

//impl ToSql for DateTime<Utc> {
//    fn to_sql(&self, ty: &Type, out: &mut Vec<u8>) -> Result<IsNull, Box<stdErr>> where
//        Self: Sized {
//        unimplemented!()
//    }
//
//    fn accepts(ty: &Type) -> bool where
//        Self: Sized {
//        unimplemented!()
//    }
//
//    fn to_sql_checked(&self, ty: &Type, out: &mut Vec<u8>) -> Result<IsNull, Box<stdErr>> {
//        unimplemented!()
//    }
//}

pub fn toGcValues(str: &String) -> Result<Vec<GcValue>, String> {
    match serde_json::from_str::<serde_json::Value>(&*str) {
        Ok(serde_json::Value::Array(elements)) => elements.iter().map(jsonToGcValue).collect(),
        Ok(json) => Err(String::from(format!(
            "provided json was not an array: {}",
            json
        ))),
        Err(e) => Err(String::from(format!("json parsing failed: {}", e))),
    }
}

pub fn toGcValue(str: String) -> Result<GcValue, String> {
    match serde_json::from_str::<serde_json::Value>(&*str) {
        Ok(result) => jsonToGcValue(&result),
        Err(e) => Err(String::from(format!("json parsing failed: {}", e))),
    }
}

fn jsonToGcValue(json: &serde_json::Value) -> Result<GcValue, String> {
    match json {
        &serde_json::Value::Object(ref map) => jsonObjecToGcValue(map),
        x => Err(format!("{} is not a valid value for a GcValue", x)),
    }
}

fn jsonObjecToGcValue(map: &serde_json::Map<String, serde_json::Value>) -> Result<GcValue, String> {
    let discriminator = map.get("discriminator").unwrap().as_str().unwrap();
    let value = map.get("value").unwrap();

    match (discriminator, value) {
        ("Int", &serde_json::Value::Number(ref n)) => Ok(GcValue::Int(n.as_i64().unwrap() as i32)),
        ("String", &serde_json::Value::String(ref s)) => Ok(GcValue::String(s.to_string())),
        ("Boolean", &serde_json::Value::Bool(b)) => Ok(GcValue::Boolean(b)),
        ("Null", &serde_json::Value::Null) => Ok(GcValue::Null),
        ("Double", &serde_json::Value::Number(ref n)) => Ok(GcValue::Double(n.as_f64().unwrap())),
        ("DateTime", &serde_json::Value::Number(ref n)) => Ok(GcValue::DateTime(n.as_i64().unwrap())),
        ("Long", &serde_json::Value::Number(ref n)) => Ok(GcValue::Long(n.as_i64().unwrap())),
        (d, v) => Err(format!(
            "discriminator {} and value {} are invalid combinations",
            d, v
        )),
    }
}

#[derive(Debug, PartialEq)]
pub enum GcValue {
    Int(i32),
    String(String),
    Boolean(bool),
    Null,
    Double(f64),
    DateTime(i64),
    Long(i64),
}

impl ToSql for GcValue {
    fn to_sql(&self, ty: &Type, out: & mut Vec<u8>) -> Result<IsNull, Box<stdErr + Sync + Send>> where
        Self: Sized {
        match self {
            GcValue::Null => Ok(IsNull::Yes),
            GcValue::DateTime(ref i) => Utc.timestamp(i / 1000, 0).naive_utc().to_sql(ty, out),
            x => panic!("ToSql match non null / datetime: {:?}", x)
        }

    }

    fn accepts(ty: & Type) -> bool where
        Self: Sized {
        true
    }

    fn to_sql_checked(&self, ty: & Type, out: & mut Vec<u8>) -> Result<IsNull, Box<stdErr + Sync + Send>> {
        Ok(IsNull::Yes)
    }
}
