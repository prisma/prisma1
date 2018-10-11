#[macro_use]
extern crate serde_derive;
extern crate jsonwebtoken as jwt;
extern crate chrono;

//use std::ffi::{CStr, CString};
use std::ffi::CStr;
use std::os::raw::c_char;
use std::mem;

use jwt::{encode, Header};
use chrono::prelude::*;

#[repr(C)]
#[no_mangle]
pub struct ProtocolBuffer {
    success: u8,
    len: usize,
    data: *const u8,
}

#[derive(Serialize)]
pub struct Claims {
    iat: i64, // Issued at
    nbf: i64, // Not before
    exp: Option<i64>, // Expiration
}

#[no_mangle]
pub extern "C" fn create_token(secret: *const c_char, expiration_in_seconds: i64) -> *mut ProtocolBuffer {
    println!("[Rust] Received: {:?}, {:?}", secret, expiration_in_seconds);

    let secret_str = to_string(secret);
    let expiration = if expiration_in_seconds < 0 {
        None
    } else {
        Some(expiration_in_seconds)
    };

    println!("[Rust] Unpacked: {}, {:?}", secret_str, expiration);

    let now = Utc::now().timestamp();
    let claims = Claims {
        iat: now,
        nbf: now,
        exp: expiration,
    };

    // String is always heap allocated, no need to box it
    let token = encode(&Header::default(), &claims, secret_str.as_ref()).unwrap();
    let len = token.len();
    let ptr = token.as_ptr();

    mem::forget(token);
    let res = Box::into_raw(Box::new(ProtocolBuffer { success: 1, len: len, data: ptr }));
    res
}

// todo free memory

/// Convert a native string to a Rust string
fn to_string(pointer: *const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    std::str::from_utf8(slice).unwrap().to_string()
}

// Convert a Rust string to a native string
//fn to_ptr(string: String) -> *const c_char {
//    let cs = CString::new(string.as_bytes()).unwrap();
//    let ptr = cs.as_ptr();
//
//    // Tell Rust not to clean up the string while we still have a pointer to it.
//    // Otherwise, we'll get a segfault.
//    mem::forget(cs);
//    ptr
//}