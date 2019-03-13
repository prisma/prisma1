#![recursion_limit = "128"]

#[macro_use]
extern crate prost_derive;

mod cursor_condition;
mod data_resolver;
mod database_executor;
mod node_selector;
mod ordering;
mod protobuf;
mod query_builder;
mod related_nodes_query_builder;
pub mod req_handlers;

use lazy_static::lazy_static;
use prisma_common::{config::PrismaConfig, error::Error};
use protobuf::{ProtoBufEnvelope, ProtoBufInterface};
use serde_yaml;
use std::{env, fs::File, slice};

pub use protobuf::prelude::*;

lazy_static! {
    pub static ref PBI: ProtoBufInterface = ProtoBufInterface::new(&CONFIG);
    pub static ref SERVER_ROOT: String = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
    pub static ref CONFIG: PrismaConfig = {
        let path = format!("{}/prisma-rs/config/prisma.yml", *SERVER_ROOT);
        serde_yaml::from_reader(File::open(path).unwrap()).unwrap()
    };
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
    count_by_model
);
