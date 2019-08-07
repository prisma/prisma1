#![recursion_limit = "128"]

#[macro_use]
extern crate prost_derive;

#[macro_use]
extern crate log;

mod error;
mod protobuf;

use error::*;
use lazy_static::lazy_static;
use prisma_common::config::{self, PrismaConfig};
use protobuf::{ProtoBufEnvelope, ProtoBufInterface};
use std::{ffi::CStr, os::raw::c_char, slice};

pub type BridgeResult<T> = Result<T, BridgeError>;

lazy_static! {
    pub static ref CONFIG: PrismaConfig = config::load().unwrap();
}

#[no_mangle]
pub unsafe extern "C" fn create_interface(database_file: *const c_char) -> *mut ProtoBufInterface {
    let database_file = CStr::from_ptr(database_file).to_str().unwrap();
    let database_file: Option<String> = if database_file == "" {
        None
    } else {
        Some(database_file.into())
    };

    let pbi = ProtoBufInterface::new(&CONFIG, database_file);
    Box::into_raw(Box::new(pbi))
}

#[no_mangle]
pub unsafe extern "C" fn destroy_interface(ptr: *mut ProtoBufInterface) {
    Box::from_raw(ptr);
}

#[no_mangle]
pub unsafe extern "C" fn destroy(buffer: *mut ProtoBufEnvelope) {
    Box::from_raw(buffer);
}

macro_rules! data_interface {
    ($($function:ident),*) => (
        pub trait ExternalInterface {
            $(
                fn $function(&self, payload: &mut [u8]) -> Vec<u8>;
            )*
        }

        $(
            #[no_mangle]
            pub unsafe extern "C" fn $function(pbi: *mut ProtoBufInterface, data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
                let pbi = Box::from_raw(pbi);
                let payload = slice::from_raw_parts_mut(data, len);
                let response_payload = pbi.$function(payload);

                Box::into_raw(pbi); // Forget the pbi, again.
                ProtoBufEnvelope::from(response_payload).into_boxed_ptr()
            }
        )*
    )
}

data_interface!(
    get_node_by_where,
    get_nodes,
    get_related_nodes,
    get_scalar_list_values_by_node_ids,
    execute_raw,
    count_by_model,
    count_by_table,
    execute_mutaction
);
