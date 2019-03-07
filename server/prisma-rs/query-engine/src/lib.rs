#![recursion_limit = "128"]

#[macro_use]
extern crate prost_derive;

pub mod cursor_condition;
pub mod data_resolvers;
pub mod ordering;
pub mod protobuf;

use lazy_static::lazy_static;
use prisma_common::{config::PrismaConfig, error::Error};
use protobuf::{ExternalInterface, ProtoBufEnvelope, ProtoBufInterface};
use serde_yaml;

// pub use protobuf::prelude::*;

use std::{env, fs::File, slice};

lazy_static! {
    pub static ref PBI: ProtoBufInterface = ProtoBufInterface::new(&CONFIG);
    pub static ref SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
    pub static ref CONFIG: PrismaConfig = {
        let path = format!("{}/prisma-rs/config/prisma.yml", *SERVER_ROOT);
        serde_yaml::from_reader(File::open(path).unwrap()).unwrap()
    };
}

#[no_mangle]
pub unsafe extern "C" fn get_node_by_where(data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
    let payload = slice::from_raw_parts_mut(data, len);
    let response_payload = PBI.get_node_by_where(payload);

    ProtoBufEnvelope::from(response_payload).into_boxed_ptr()
}

#[no_mangle]
pub unsafe extern "C" fn get_nodes(data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
    let payload = slice::from_raw_parts_mut(data, len);
    let response_payload = PBI.get_nodes(payload);

    ProtoBufEnvelope::from(response_payload).into_boxed_ptr()
}

#[no_mangle]
pub unsafe extern "C" fn destroy(buffer: *mut ProtoBufEnvelope) {
    Box::from_raw(buffer);
}
