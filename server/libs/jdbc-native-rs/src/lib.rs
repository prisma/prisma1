#![allow(non_snake_case, unused_mut)]

extern crate serde;
extern crate serde_json;
extern crate postgres;
extern crate colored;

use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::str;
use colored::*;

pub mod driver;

#[no_mangle]
pub extern "C" fn newConnection<'a>(url: *const c_char) -> *mut driver::PsqlConnection<'a> {
    let mut connection = driver::connect(to_string(url));
    let ptr = Box::into_raw(Box::new(connection));

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
    let result = conn.queryRawParams(queryString, paramsString);

    return to_ptr(handleResult(result, String::from("[]")).to_string());
}

#[no_mangle]
pub extern "C" fn sqlExecute(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) {
    println!("[Rust] Calling exec");
    let queryString = to_string(query);
    let paramsString = to_string(params);

    handleResult(conn.executeRawParams(queryString, paramsString), 0);
}

fn handleResult<T>(result: driver::Result<T>, default: T) -> T {
    match result {
        Ok(v) => v,
        Err(e) => {
            let cstr = format!("[Rust ERROR] {:?}", e);
            println!("{}", cstr.red());
            default
        },
    }
}

#[no_mangle]
pub extern "C" fn closeConnection(conn: *mut driver::PsqlConnection) {
    let wat = unsafe { Box::from_raw(conn) };
    wat.close();
}

#[no_mangle]
pub extern "C" fn startTransaction<'a>(conn: *mut driver::PsqlConnection) {
    unsafe {
        let res = (*conn).startTransaction();
        handleResult(res, ())
    }
}

#[no_mangle]
pub extern "C" fn commitTransaction(conn: *mut driver::PsqlConnection) {
    println!("[Rust] committing");
    let ptr = unsafe { Box::from_raw(conn) };
    handleResult(ptr.commitTransaction(), ());
    mem::forget(ptr);
    println!("[Rust] committed");
}

#[no_mangle]
pub extern "C" fn rollbackTransaction(conn: *mut driver::PsqlConnection) {
    println!("[Rust] Rolling back");
    let ptr = unsafe { Box::from_raw(conn) };
    handleResult(ptr.rollbackTransaction(), ());
    mem::forget(ptr);
    println!("[Rust] Rolled back");
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
