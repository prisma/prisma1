#![allow(non_snake_case, unused_mut)]

extern crate serde;
extern crate serde_json;
extern crate postgres;
extern crate colored;
extern crate chrono;
extern crate byteorder;
extern crate rust_decimal;
extern crate num_traits;

#[macro_use]
extern crate serde_derive;

use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::str;
use colored::*;
use postgres::rows::Rows;

mod driver;
mod serialization;
mod jdbc_params;

use serialization::ResultSet;

#[no_mangle]
pub extern "C" fn newConnection<'a>(url: *const c_char) -> *mut driver::PsqlConnection<'a> {
    let mut connection = driver::connect(to_string(url));
    let ptr = Box::into_raw(Box::new(connection));

    return ptr;
}

#[no_mangle]
pub extern "C" fn prepareStatement<'a>(conn: &'a driver::PsqlConnection<'a>, query: *const c_char) -> *mut driver::PsqlPreparedStatement<'a> {
    let mut stmt = conn.prepareStatement(to_string(query)).unwrap(); // todo req better err handling struct { ptr string }
    let ptr = Box::into_raw(Box::new(stmt));

    return ptr;
}

#[no_mangle]
pub extern "C" fn sqlQuery(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) -> *const c_char {
    let queryString = to_string(query);
    let paramsString = to_string(params);
    let callResult = jdbc_params::toJdbcParameters(&paramsString).and_then(|p| {
        conn.query(queryString, p.iter().collect())
    }).and_then(|rows| {
        CallResult::result_set(rows)
    });

    return serializeCallResult(callResult);
}

#[no_mangle]
pub extern "C" fn executePreparedstatement(
    stmt: &driver::PsqlPreparedStatement,
    params: *const c_char,
) -> *const c_char {
    println!("[Rust] Calling exec on prepared statement");
    let paramsString = to_string(params);
    let callResult = jdbc_params::toJdbcParameterList(&paramsString).and_then(|p| {
        stmt.execute(p.iter().map(|x| x.iter().collect()).collect())
    }).map(|x| {
        CallResult::count(x as i32)
    });

    return serializeCallResult(callResult);
}

#[no_mangle]
pub extern "C" fn sqlExecute(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) -> *const c_char {
    println!("[Rust] Calling exec");
    let queryString = to_string(query);
    let paramsString = to_string(params);
    let callResult = jdbc_params::toJdbcParameters(&paramsString).and_then(|p| {
        conn.execute(queryString, p.iter().collect())
    }).map(|x| {
        CallResult::count(x as i32)
    });

    return serializeCallResult(callResult);
}

#[derive(Serialize)]
struct CallResult {
    ty: String,
    rows: Option<ResultSet>,
    error: Option<CallError>,
    count: Option<i32>
}

impl CallResult {
    pub fn count(c: i32) -> CallResult {
        CallResult {
            ty: String::from("COUNT"),
            rows: None,
            error: None,
            count: Some(c)
        }
    }

    pub fn result_set(rows: Rows) -> driver::Result<CallResult> {
        let data = ResultSet::create(rows)?;
        Ok(CallResult {
            ty: String::from("RESULT_SET"),
            rows: Some(data),
            error: None,
            count: None
        })
    }

    pub fn empty() -> CallResult {
        CallResult {
            ty: String::from("EMPTY"),
            rows: None,
            error: None,
            count: None
        }
    }

    pub fn error(code: String, message: String) -> CallResult {
        CallResult {
            ty: String::from("ERROR"),
            rows: None,
            error: Some(CallError {
                code: code,
                message: message,
            }),
            count: None
        }
    }
}

#[derive(Serialize)]
struct CallError {
    code: String,
    message: String
}

fn serializeCallResult(res: driver::Result<CallResult>) -> *const c_char {
    let result = handleResult(res);
    let serialized = serde_json::to_string(&result).unwrap();
    to_ptr(serialized)
}

fn handleResult(result: driver::Result<CallResult>) -> CallResult {
    match result {
        Ok(v) => v,
        Err(e) => {
            let err = format!("[Rust ERROR] {:?}", e);
            println!("{}", err.red());

            match e {
                driver::DriverError::PsqlError(ref err) => match err.as_db() {
                  Some(dbErr) => CallResult::error(String::from(dbErr.code.code()), dbErr.message.clone()),
                  None =>panic!("No PSQL error, something else: {:?}", e),
                },

                _ => CallResult::error(String::from("-1"), err),
            }
        }
    }
}

#[no_mangle]
pub extern "C" fn closeConnection(conn: *mut driver::PsqlConnection) -> *const c_char  {
    let connection = unsafe { Box::from_raw(conn) };
    connection.close();

    return serializeCallResult(Ok(CallResult::empty()));
}

#[no_mangle]
pub extern "C" fn startTransaction<'a>(conn: *mut driver::PsqlConnection) -> *const c_char  {
    unsafe {
        let res = (*conn).startTransaction();
        return serializeCallResult(res.map(|_| { CallResult::empty() }));
    }
}

#[no_mangle]
pub extern "C" fn commitTransaction(conn: *mut driver::PsqlConnection) -> *const c_char  {
    println!("[Rust] committing");
    let ptr = unsafe { Box::from_raw(conn) };
    let ret = serializeCallResult(ptr.commitTransaction().map(|_| { CallResult::empty() }));
    mem::forget(ptr);
    println!("[Rust] committed");

    ret
}

#[no_mangle]
pub extern "C" fn rollbackTransaction(conn: *mut driver::PsqlConnection) -> *const c_char  {
    println!("[Rust] Rolling back");
    let ptr = unsafe { Box::from_raw(conn) };
    let ret = serializeCallResult(ptr.rollbackTransaction().map(|_| { CallResult::empty() }));

    mem::forget(ptr);
    println!("[Rust] Rolled back");

    ret
}

/// Convert a native string to a Rust string
fn to_string(pointer: *const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    str::from_utf8(slice).unwrap().to_string()
}

/// Convert a Rust string to a native string
fn to_ptr(string: String) -> *const c_char {
    let cs = CString::new(string.as_bytes()).unwrap();
    let ptr = cs.as_ptr();
    // Tell Rust not to clean up the string while we still have a pointer to it.
    // Otherwise, we'll get a segfault.

    mem::forget(cs);
    ptr
}
