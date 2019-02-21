#![allow(non_snake_case, unused, unused_mut)]

extern crate serde;
extern crate serde_json;
extern crate postgres;
extern crate colored;
extern crate chrono;
extern crate byteorder;
extern crate rust_decimal;
extern crate num_traits;
extern crate uuid;

#[macro_use]
extern crate log;

#[macro_use]
extern crate serde_derive;
extern crate postgres_array;

use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::str;
use colored::*;
use postgres::rows::Rows;

mod driver;
mod serialization;
mod jdbc_params;
mod logging;

use serialization::ResultSet;
use driver::PointerAndError;

#[no_mangle]
pub extern "C" fn jdbc_initialize() {
    logging::init();
}

#[no_mangle]
pub extern "C" fn newConnection<'a>(url: *const c_char) -> *mut driver::PsqlConnection<'a> {
    let mut connection = driver::connect(to_string(url));
    let ptr = Box::into_raw(Box::new(connection));
    trace!("New connection - handing out: {:?}", ptr);
    return ptr;
}

#[no_mangle]
pub extern "C" fn prepareStatement<'a>(conn: &'a driver::PsqlConnection<'a>, query: *const c_char) -> *mut PointerAndError {
    trace!("Preparing query: {}", to_string(query));

    let pointerAndError = match conn.prepareStatement(to_string(query)) {
        Ok(pStmt) => PointerAndError {
            error: serializeCallResult(Ok(CallResult::empty())),
            pointer: Box::into_raw(Box::new(pStmt)),
        },

        Err(e) => PointerAndError {
            error: serializeCallResult(Ok(errorToCallResult(e))),
            pointer: std::ptr::null_mut(),
        }
    };

    let ptr = Box::into_raw(Box::new(pointerAndError));
    trace!("Prepare - handing out: {:?}", ptr);
    ptr
}

#[no_mangle]
pub extern "C" fn closeStatement(stmt: *mut driver::PsqlPreparedStatement) -> *const c_char  {
    trace!("Closing statement: {:?}", stmt);
    let boxedStmt = unsafe { Box::from_raw(stmt) };
    let ptr = serializeCallResult(Ok(CallResult::empty()));
    trace!("Close statement - handing out: {:?}", ptr);
    ptr
}


#[no_mangle]
pub extern "C" fn executePreparedstatement(
    stmt: &driver::PsqlPreparedStatement,
    params: *const c_char,
) -> *const c_char {
    let paramsString = to_string(params);
    let callResult = jdbc_params::toJdbcParameterList(&paramsString).and_then(|p| {
        stmt.execute(p.iter().map(|x| x.iter().collect()).collect())
    }).map(|x: Vec<i32>| {
        CallResult::count(x)
    });

    let ptr = serializeCallResult(callResult);
    trace!("Exec prepared result - handing out: {:?}", ptr);
    ptr
}

#[no_mangle]
pub extern "C" fn queryPreparedstatement(
    stmt: &driver::PsqlPreparedStatement,
    params: *const c_char,
) -> *const c_char {
    let paramsString = to_string(params);
    let callResult = jdbc_params::toJdbcParameters(&paramsString).and_then(|p| {
        stmt.query(p.iter().collect())
    }).and_then(|rows| {
        CallResult::result_set(rows)
    });

    let ptr = serializeCallResult(callResult);
    trace!("Query prepared result - handing out: {:?}", ptr);
    ptr
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

    let ptr = serializeCallResult(callResult);
    trace!("Query result - handing out: {:?}", ptr);
    ptr
}

#[no_mangle]
pub extern "C" fn sqlExecute(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) -> *const c_char {
    let queryString = to_string(query);
    let paramsString = to_string(params);
    let callResult = jdbc_params::toJdbcParameters(&paramsString).and_then(|p| {
        conn.execute(queryString, p.iter().collect())
    }).map(|x| {
        CallResult::count(vec!(x as i32))
    });

    let ptr = serializeCallResult(callResult);
    trace!("Exec result - handing out: {:?}", ptr);
    ptr
}

#[derive(Serialize)]
struct CallResult {
    ty: String,
    rows: Option<ResultSet>,
    error: Option<CallError>,
    counts: Vec<i32>,
}

impl CallResult {
    pub fn count(c: Vec<i32>) -> CallResult {
        CallResult {
            ty: String::from("COUNT"),
            rows: None,
            error: None,
            counts: c
        }
    }

    pub fn result_set(rows: Rows) -> driver::Result<CallResult> {
        let data = ResultSet::create(rows)?;
        Ok(CallResult {
            ty: String::from("RESULT_SET"),
            rows: Some(data),
            error: None,
            counts: Vec::new()
        })
    }

    pub fn empty() -> CallResult {
        CallResult {
            ty: String::from("EMPTY"),
            rows: None,
            error: None,
            counts: Vec::new()
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
            counts: Vec::new()
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
        Err(e) => errorToCallResult(e),
    }
}

fn errorToCallResult(e: driver::DriverError) -> CallResult {
    let err = format!("[Rust] {:?}", e);
    trace!("{}", err);

    match e {
        driver::DriverError::PsqlError(ref e) => match e.as_db() {
            Some(dbErr) => CallResult::error(String::from(dbErr.code.code()), dbErr.message.clone()),
            None => CallResult::error(String::from("-1"), err),
        },

        _ => CallResult::error(String::from("-2"), err),
    }
}

#[no_mangle]
pub extern "C" fn closeConnection(conn: *mut driver::PsqlConnection) -> *const c_char  {
    trace!("Closing connection: {:?}", conn);
    let connection = unsafe { Box::from_raw(conn) };
    connection.close();

    let ptr = serializeCallResult(Ok(CallResult::empty()));
    trace!("Close connection - handing out: {:?}", ptr);
    ptr
}

#[no_mangle]
pub extern "C" fn destroy(pointerAndError: *mut PointerAndError) {
    trace!("Destroying PointerAndError: {:?}", pointerAndError);
    unsafe {
        Box::from_raw(pointerAndError)
    };
}

#[no_mangle]
pub extern "C" fn destroy_string(s: *mut c_char) {
    trace!("Dropping string buffer: {:?}", s);
    unsafe {
        String::from_utf8(CString::from_raw(s).into_bytes()).unwrap()
    };
}

#[no_mangle]
pub extern "C" fn startTransaction<'a>(conn: *mut driver::PsqlConnection) -> *const c_char  {
    unsafe {
        let res = (*conn).startTransaction();
        let ptr = serializeCallResult(res.map(|_| { CallResult::empty() }));
        trace!("Opened transaction on {:?} - handing out: {:?}", conn, ptr);
        ptr
    }
}

#[no_mangle]
pub extern "C" fn commitTransaction(conn: *mut driver::PsqlConnection) -> *const c_char  {
    let ptr = unsafe { Box::from_raw(conn) };
    let ret = serializeCallResult(ptr.commitTransaction().map(|_| { CallResult::empty() }));

    mem::forget(ptr);
    trace!("Committed on connection {:?} - handing out: {:?}", conn, ret);
    ret
}

#[no_mangle]
pub extern "C" fn rollbackTransaction(conn: *mut driver::PsqlConnection) -> *const c_char  {
    let ptr = unsafe { Box::from_raw(conn) };
    let ret = serializeCallResult(ptr.rollbackTransaction().map(|_| { CallResult::empty() }));

    mem::forget(ptr);
    trace!("Rolled back on connection {:?} - handing out: {:?}", conn, ret);
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
