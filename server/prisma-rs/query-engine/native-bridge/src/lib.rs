#![deny(warnings)]
#![recursion_limit = "128"]

#[macro_use]
extern crate prost_derive;

mod protobuf;

use lazy_static::lazy_static;
use prisma_common::{
    config::{self, PrismaConfig},
    error::Error,
};

use protobuf::{ProtoBufEnvelope, ProtoBufInterface};
use std::{env, slice};

lazy_static! {
    pub static ref PBI: ProtoBufInterface = ProtoBufInterface::new(&CONFIG);
    pub static ref SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
    pub static ref CONFIG: PrismaConfig = config::load().unwrap();
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
            pub unsafe extern "C" fn $function(data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
                let payload = slice::from_raw_parts_mut(data, len);
                let response_payload = PBI.$function(payload);

                ProtoBufEnvelope::from(response_payload).into_boxed_ptr()
            }
        )*
    )
}

#[no_mangle]
pub unsafe extern "C" fn destroy(buffer: *mut ProtoBufEnvelope) {
    Box::from_raw(buffer);
}

data_interface!(
    get_node_by_where,
    get_nodes,
    get_related_nodes,
    get_scalar_list_values_by_node_ids,
    execute_raw,
    count_by_model,
    count_by_table
);
