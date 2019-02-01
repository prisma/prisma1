use crate::{
    config::{ConnectionLimit, PrismaConfig, PrismaDatabase},
    connector::{Connector, Sqlite},
    error::Error,
    project::Project,
    querying::NodeSelector,
    PrismaResult,
};

use prost::Message;
use std::mem;

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

use prisma::get_node_by_where_response::Response;

#[repr(C)]
#[no_mangle]
pub struct ProtoBufEnvelope {
    pub data: *mut u8,
    pub len: usize,
}

impl ProtoBufEnvelope {
    pub fn into_boxed_ptr(self) -> *mut ProtoBufEnvelope {
        Box::into_raw(Box::new(self))
    }
}

impl Drop for ProtoBufEnvelope {
    fn drop(&mut self) {
        if self.len > 0 {
            unsafe {
                drop(Box::from_raw(self.data));
            };
        }
    }
}

impl From<Vec<u8>> for ProtoBufEnvelope {
    fn from(mut v: Vec<u8>) -> Self {
        let len = v.len();
        let data = v.as_mut_ptr();

        mem::forget(v);
        ProtoBufEnvelope { data, len }
    }
}

pub struct ProtobufInterface {
    connector: Box<dyn Connector + Send + Sync + 'static>,
}

impl ProtobufInterface {
    pub fn new(config: &PrismaConfig) -> ProtobufInterface {
        let connector = match config.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite" => {
                Sqlite::new(config.limit()).unwrap()
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };
        
        ProtobufInterface { connector: Box::new(connector), }
    }
    
    fn protobuf_result<F>(f: F) -> Vec<u8>
    where
        F: FnOnce() -> PrismaResult<Vec<u8>>
    {
        f().unwrap_or_else(|error| {
            let error_response = prisma::GetNodeByWhereResponse {
                header: prisma::Header {
                    type_name: String::from("GetNodeByWhereResponse"),
                },
                response: Some(Response::Error(prisma::Error { value: Some(error.into()) }))
            };
            
            let mut payload = Vec::new();
            error_response.encode(&mut payload).unwrap();
            
            payload
        })
    }
    
    pub fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let params = prisma::GetNodeByWhere::decode(payload)?;
            let project: Project = serde_json::from_reader(params.project_json.as_slice())?;

            let model = project
                .schema
                .find_model(&params.model_name)
                .ok_or_else(|| Error::InvalidInputError(format!("Model not found: {}", params.model_name)))?;

            let field = model
                .find_field(&params.field_name)
                .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", params.field_name)))?;

            let value = params
                .value
                .prisma_value
                .ok_or_else(|| Error::InvalidInputError(String::from("Search value cannot be empty.")))?;
            
            let selected_fields = model.fields.as_slice();

            let node_selector = NodeSelector::new(
                project.db_name(),
                model,
                field,
                &value,
                model.fields.as_slice(),
            );

            let result = self.connector.get_node_by_where(project.db_name(), &node_selector)?;

            let response_values = result
                .into_iter()
                .map(|value| prisma::ValueContainer {
                    prisma_value: Some(value),
                })
                .collect();

            let response = prisma::GetNodeByWhereResponse {
                header: prisma::Header {
                    type_name: String::from("GetNodeByWhereResponse"),
                },
                response: Some(Response::Result(prisma::Result {
                    values: response_values,
                    fields: selected_fields.iter().map(|field| field.name.clone()).collect()
                })),
            };

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();
            
            Ok(response_payload)
        })
    }
}
