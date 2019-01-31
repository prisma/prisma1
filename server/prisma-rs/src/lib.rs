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

use config::{ConnectionLimit, PrismaConfig, PrismaDatabase};
use connector::{Connector, Sqlite};
use error::Error;
use project::Project;
use prost::Message;
use querying::NodeSelector;
use serde_yaml;

use protobuf::{
    prisma::{GetNodeByWhere, GetNodeByWhereResponse, Header, ValueContainer},
    ProtoBufEnvelope,
};

pub use protobuf::prisma::value_container::PrismaValue;

use std::{env, fs::File, slice};

type PrismaResult<T> = Result<T, Error>;

lazy_static! {
    pub static ref SQLITE: Sqlite = {
        match CONFIG.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite" => {
                connector::Sqlite::new(config.limit()).unwrap()
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        }
    };
    pub static ref CONFIG: PrismaConfig = {
        let root = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));
        let path = format!("{}/prisma-rs/config/prisma.yml", root);

        dbg!(&path);

        serde_yaml::from_reader(File::open(path).unwrap()).unwrap()
    };
}

#[no_mangle]
pub extern "C" fn select_1() -> i32 {
    SQLITE.select_1().unwrap()
}

#[no_mangle]
pub extern "C" fn get_node_by_where(data: *mut u8, len: usize) -> *mut ProtoBufEnvelope {
    let payload = unsafe { slice::from_raw_parts_mut(data, len) };

    // Q: Does this mean that the JVM-owned memory is freed when this is dropped? That would cause a double-free.
    let params = GetNodeByWhere::decode(payload).unwrap();
    let project: Project = serde_json::from_reader(params.project_json.as_slice()).unwrap();
    let model = project.schema.find_model(&params.model_name).unwrap();
    let field = model.find_field(&params.field_name).unwrap();
    let value = params.value.prisma_value.unwrap();

    let node_selector = NodeSelector::new(
        project.db_name(),
        model,
        field,
        &value,
        model.fields.as_slice(),
    );

    let result = SQLITE.get_node_by_where(project.db_name(), &node_selector);

    let response_values = result
        .unwrap()
        .into_iter()
        .map(|value| ValueContainer {
            prisma_value: Some(value),
        })
        .collect();

    let response = GetNodeByWhereResponse {
        header: Header {
            type_name: String::from("GetNodeByWhereResponse"),
        },
        response: response_values,
    };

    let mut payload = Vec::new();
    response.encode(&mut payload).unwrap();

    ProtoBufEnvelope::from(payload).into_boxed_ptr()
}

#[no_mangle]
pub extern "C" fn destroy(buffer: *mut ProtoBufEnvelope) {
    unsafe {
        Box::from_raw(buffer)
    };
}