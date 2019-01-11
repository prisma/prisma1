#![allow(unused_variables, dead_code, unused_imports, unused_mut)]

#[macro_use]
extern crate serde_derive;
extern crate jsonwebtoken as jwt;
extern crate chrono;

#[macro_use]
extern crate log;

mod protocol_buffer;
mod ffi_utils;
mod grant;
mod logging;

use std::os::raw::c_char;
use std::str::FromStr;
use std::error::Error;
use jwt::{decode, encode, Header, Validation, Algorithm};
use chrono::prelude::*;
use ffi_utils::{to_str, to_string, str_vec_from_pointers};
use protocol_buffer::ProtocolBuffer;
use grant::Grant;

pub type Result<T> = std::result::Result<T, ProtocolError>;

pub enum ProtocolError {
    GenericError(String)
}

#[derive(Serialize, Deserialize)]
pub struct Claims {
    #[serde(skip_serializing_if = "Option::is_none")]
    iat: Option<i64>, // Issued at

    #[serde(skip_serializing_if = "Option::is_none")]
    nbf: Option<i64>, // Not before

    #[serde(skip_serializing_if = "Option::is_none")]
    exp: Option<i64>, // Expiration

    #[serde(skip_serializing_if = "Option::is_none")]
    grants: Option<Vec<Grant>>,
}

#[no_mangle]
pub extern "C" fn jwt_initialize() {
    logging::init();
}

#[no_mangle]
pub extern "C" fn create_token(algorithm: *const c_char, secret: *const c_char, expiration_in_seconds: i64, allowed_target: *const c_char, allowed_action: *const c_char) -> *mut ProtocolBuffer {
    let alg_string = to_str(algorithm);
    let use_algorithm = match Algorithm::from_str(alg_string) {
        Ok(a) => a,
        Err(e) => return ProtocolBuffer::from(ProtocolError::GenericError(format!("Invalid algorithm: {}", alg_string))).into_boxed_ptr()
    };

    let secret_str = to_str(secret);
    let expiration = if expiration_in_seconds < 0 {
        None
    } else {
        Some(expiration_in_seconds)
    };

    let grant_to_encode = Grant::from(allowed_target, allowed_action).map(|g| vec!(g));
    let now = Utc::now().timestamp();
    let claims = Claims {
        iat: Some(now),
        nbf: Some(now),
        exp: expiration,
        grants: grant_to_encode,
    };

    let header = Header::new(use_algorithm);
    let token = encode( &header, &claims, secret_str.as_ref()).unwrap();
    let ptr = ProtocolBuffer::from(token).into_boxed_ptr();

    trace!("Create - handing out: {:?}", ptr);
    ptr
}

#[no_mangle]
pub extern "C" fn verify_token(token: *const c_char, secrets: *const *const c_char, num_secrets: i64, expect_target: *const c_char, expect_action: *const c_char) -> *mut ProtocolBuffer {
    let parsed_token = to_str(token);
    let parsed_secrets = str_vec_from_pointers(secrets, num_secrets);
    let mut last_error: String = String::from("");

    for secret in parsed_secrets {
        let t = decode::<Claims>(parsed_token, secret.as_ref(), &Validation { validate_exp: false, ..Validation::default()});
        match t {
            Ok(x) => {
                return validate_claims(x.claims, Grant::from(expect_target, expect_action)).into_boxed_ptr();
            },
            Err(e) => {
                let err = format!("{}", e);
                if last_error != err {
                    last_error = err;
                }
            },
        }
    }

    let ptr = ProtocolBuffer::from(ProtocolError::GenericError(String::from(last_error))).into_boxed_ptr();

    trace!("Verify - handing out: {:?}", ptr);
    ptr
}

#[no_mangle]
pub extern "C" fn destroy_buffer(buffer: *mut ProtocolBuffer) {
    trace!("Dropping buffer at {:?}", buffer);
    unsafe { Box::from_raw(buffer) };
}

fn validate_claims(claims: Claims, grant: Option<Grant>) -> ProtocolBuffer {
    if is_expired(claims.exp) {
        return ProtocolBuffer::from(ProtocolError::GenericError(String::from("Token is expired.")));
    }

    if is_issued_in_future(claims.iat) {
        return ProtocolBuffer::from(ProtocolError::GenericError(format!("Token is issued in the future (iat).")));
    }

    if is_used_before_validity(claims.nbf) {
        return ProtocolBuffer::from(ProtocolError::GenericError(format!("Token is not yet valid (nbf in the future).")));
    }

    match contains_valid_grant(&grant, &claims.grants) {
        Ok(valid) if !valid => return ProtocolBuffer::from(ProtocolError::GenericError(format!("Token grants do not satisfy the request. Got: {:?} Required: {:?}", claims.grants, grant))),
        Err(e)              => return ProtocolBuffer::from(e),
        _                   => (),
    }

    return ProtocolBuffer::from(true)
}

fn is_expired(exp_claim: Option<i64>) -> bool {
    match exp_claim {
        Some(exp) => exp < Utc::now().timestamp(),
        None      => false,
    }
}

fn is_used_before_validity(nbf_claim: Option<i64>) -> bool{
    match nbf_claim {
        Some(nbf) => nbf > Utc::now().timestamp(),
        None      => false,
    }
}

fn is_issued_in_future(iat_claim: Option<i64>) -> bool{
    match iat_claim {
        Some(iat) => iat > Utc::now().timestamp(),
        None      => false,
    }
}

fn contains_valid_grant(expected: &Option<Grant>, contained: &Option<Vec<Grant>>) -> Result<bool> {
    match (expected, contained) {
        (None, _) => Ok(true),
        (Some(ref ex), Some(ref grants)) if grants.len() > 0 => {
            for g in grants {
                if g.fulfills(ex)? { return Ok(true); }
            }

            return Ok(false);
        },
        _ => Ok(false),
    }
}
