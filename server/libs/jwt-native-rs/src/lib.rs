#![allow(unused_variables, dead_code)]

#[macro_use]
extern crate serde_derive;
extern crate jsonwebtoken as jwt;
extern crate chrono;

use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::mem;

use jwt::{decode, encode, Header, Validation};
use chrono::prelude::*;

#[repr(C)]
#[no_mangle]
pub struct ProtocolBuffer {
    success: u8,
    len: usize,
    data: *const c_char,
}

impl Drop for ProtocolBuffer {
    fn drop(&mut self) {
        println!("[Rust] Dropping ProtocolBuffer");
    }
}

#[derive(Serialize, Deserialize)]
pub struct Claims {
    iat: i64, // Issued at
    nbf: i64, // Not before

    #[serde(skip_serializing_if = "Option::is_none")]
    exp: Option<i64>, // Expiration
}

#[no_mangle]
pub extern "C" fn create_token(secret: *const c_char, expiration_in_seconds: i64) -> *mut ProtocolBuffer {
    let secret_str = to_string(secret);
    let expiration = if expiration_in_seconds < 0 {
        None
    } else {
        Some(expiration_in_seconds)
    };

    let now = Utc::now().timestamp();
    let claims = Claims {
        iat: now,
        nbf: now,
        exp: expiration,
    };

    // String is always heap allocated, no need to box it
    let token = encode(&Header::default(), &claims, secret_str.as_ref()).unwrap();

    Box::into_raw(write_string_buffer(true, token))
}

#[no_mangle]
pub extern "C" fn verify_token(token: *const c_char, secrets: *const c_char, num_secrets: i64) -> *mut ProtocolBuffer {
    let parsed_token = to_string(token);
    let parsed_secrets = to_string_vector(secrets, num_secrets);

    let mut last_error: String = String::from("");

    for secret in parsed_secrets {
        let t = decode::<Claims>(&parsed_token, secret.as_ref(), &Validation { validate_exp: false, ..Validation::default()});
        match t {
            Ok(x) => {
                if is_expired(x.claims.exp) {
                    return Box::into_raw(write_string_buffer(false, String::from("token is expired")))
                }
                return Box::into_raw(write_empty_buffer(true))
            },
            Err(e) => last_error.push_str(&format!("{} ", e)),
        }
    }

    println!("[Rust] Failed");
    Box::into_raw(write_string_buffer(false, last_error))
}

#[no_mangle]
pub extern "C" fn destroy_buffer(buffer: *mut ProtocolBuffer) {
    unsafe { Box::from_raw(buffer) };
}

fn is_expired(exp_claim: Option<i64>) -> bool {
    match exp_claim {
        Some(exp) => {
            println!("{}", exp);
            exp >= Utc::now().timestamp()
        },
        None => false,
    }
}

// Rust -> JVM

fn write_empty_buffer(success: bool) -> Box<ProtocolBuffer> {
    Box::new(ProtocolBuffer { success: success as u8, len: 0, data: std::ptr::null() })
}

fn write_string_buffer(success: bool, data: String) -> Box<ProtocolBuffer> {
    let len = data.len();
    let ptr = string_to_ptr(data);

    Box::new(ProtocolBuffer { success: success as u8, len: len, data: ptr })
}

fn string_to_ptr(s: String) -> *const c_char {
    let cs = CString::new(s.as_bytes()).unwrap();
    let ptr = cs.as_ptr();

    mem::forget(cs);
    ptr
}

// JVM -> Rust

// Todo verify that this does 0 copy
fn to_string(pointer: *const c_char) -> String {
    unsafe {
        String::from(std::str::from_utf8_unchecked(CStr::from_ptr(pointer).to_bytes()))
    }
}

// Todo verify that this does 0 copy
fn to_string_vector(raw: *const c_char, num_elements: i64) -> Vec<String> {
    let mut vec = Vec::new();
    unsafe {
        for offset in 0..num_elements {
            let ptr = { raw.offset(offset as isize) };
            vec.push(to_string(ptr))
        }
    }

    vec
}

