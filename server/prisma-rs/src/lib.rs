#![recursion_limit = "128"]

#[macro_use]
extern crate serde_derive;
#[macro_use]
extern crate prost_derive;
#[macro_use]
extern crate debug_stub_derive;

pub mod config;
pub mod connectors;
pub mod error;
pub mod executor;
pub mod models;
pub mod protobuf;

use config::PrismaConfig;
use error::Error;
use lazy_static::lazy_static;
use protobuf::{ProtoBufEnvelope, ProtoBufInterface, ScalaInterface};
use serde_yaml;

pub use protobuf::prisma::value_container::PrismaValue;

use std::{env, fs::File, slice};

type PrismaResult<T> = Result<T, Error>;

lazy_static! {
    pub static ref PBI: ProtoBufInterface = ProtoBufInterface::new(&CONFIG);
    pub static ref SERVER_ROOT: String =
        env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
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
