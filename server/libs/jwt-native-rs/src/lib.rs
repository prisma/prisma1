#![allow(unused_variables, dead_code)]

#[macro_use]
extern crate serde_derive;
extern crate jsonwebtoken as jwt;
extern crate chrono;

use std::os::raw::c_char;
use jwt::{decode, encode, Header, Validation};
use chrono::prelude::*;

mod protocol_buffer;
mod ffi_utils;

use ffi_utils::{to_string, to_string_vector};
use protocol_buffer::{ProtocolBuffer, ProtocolError};

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

    let token = encode(&Header::default(), &claims, secret_str.as_ref()).unwrap();
    ProtocolBuffer::from(token).into_boxed_ptr()
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
                    return ProtocolBuffer::from(ProtocolError::GenericError(String::from("token is expired"))).into_boxed_ptr();
                }
                return ProtocolBuffer::from(true).into_boxed_ptr()
            },
            Err(e) => last_error.push_str(&format!("{} ", e)),
        }
    }

    println!("FAILED");
    ProtocolBuffer::from(ProtocolError::GenericError(String::from(last_error))).into_boxed_ptr()
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
