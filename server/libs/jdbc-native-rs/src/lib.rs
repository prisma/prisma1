#![allow(non_snake_case, unused_mut)]

extern crate serde;
extern crate serde_json;
extern crate postgres;
//extern crate serde_derive;

use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::str;
use std::panic;
use std::sync::Mutex;

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
    let params = driver::toGcValues(&paramsString).expect(&*format!(
        "could not convert gc values successfully: {}",
        &paramsString
    ));

    let result = conn.query(queryString, params.iter().collect());
    return to_ptr(result.to_string());
}

#[no_mangle]
pub extern "C" fn sqlExecute(
    conn: &driver::PsqlConnection,
    query: *const c_char,
    params: *const c_char,
) {
    println!("Calling exec");
    let queryString = to_string(query);
    let paramsString = to_string(params);
    let mutex = Mutex::new(conn);

    let result = panic::catch_unwind(||{
        let x = mutex.lock().unwrap();

        let params = driver::toGcValues(&paramsString).expect(&*format!(
            "could not convert gc values successfully: {}",
            &paramsString
        ));
        (*x).execute(queryString, params.iter().collect());
    });

    handle(result)
}

fn handle(r: std::thread::Result<()>) {
    match r {
        Ok(r) => println!("All is well! {:?}", r),
        Err(e) => {
//            println!("typeid of error: {:?}", e.get_type_id());
            if let Some(e) = e.downcast_ref::<self::postgres::Error>() {
                println!("Got an error: {}", e);
            } else {
                println!("Got an unknown error: {:?}", e);
            }
        }
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
        (*conn).startTransaction();
    }
}

#[no_mangle]
pub extern "C" fn commitTransaction(conn: *mut driver::PsqlConnection) {
    println!("[Rust] committing");
    let wat = unsafe { Box::from_raw(conn) };
    wat.commitTransaction();
    mem::forget(wat);
    println!("[Rust] committed");
}

#[no_mangle]
pub extern "C" fn rollbackTransaction(conn: *mut driver::PsqlConnection) {
    println!("[Rust] Rolling back");
    let wat = unsafe { Box::from_raw(conn) };
    wat.rollbackTransaction();
    mem::forget(wat);
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
