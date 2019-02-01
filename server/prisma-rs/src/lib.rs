#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate serde_derive;
#[macro_use]
extern crate prost_derive;

mod config;
mod connector;
mod error;
mod project;
mod protobuf;
mod querying;
mod schema;

use config::PrismaConfig;
use error::Error;
use serde_yaml;

use protobuf::{
    ProtoBufEnvelope,
    ProtobufInterface,
};

pub use protobuf::prisma::value_container::PrismaValue;

use std::{env, fs::File, slice};

type PrismaResult<T> = Result<T, Error>;

lazy_static! {
    pub static ref PBI: ProtobufInterface = ProtobufInterface::new(&CONFIG);

    pub static ref CONFIG: PrismaConfig = {
        let root = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
        let path = format!("{}/prisma-rs/config/prisma.yml", root);

        dbg!(&path);

        serde_yaml::from_reader(File::open(path).unwrap()).unwrap()
    };
}

#[no_mangle]
pub extern "C" fn get_node_by_where(data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
    let payload = unsafe { slice::from_raw_parts_mut(data, len) };
    let response_payload = PBI.get_node_by_where(payload);

    ProtoBufEnvelope::from(response_payload).into_boxed_ptr()
}

#[no_mangle]
pub extern "C" fn destroy(buffer: *mut ProtoBufEnvelope) {
    unsafe {
        Box::from_raw(buffer)
    };
}
