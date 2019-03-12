use crate::{
    data_resolver::{DataResolver, SqlResolver},
    database_executor::Sqlite,
    node_selector::NodeSelector,
    protobuf::{prelude::*, InputValidation},
};
use prisma_common::{config::*, error::Error, PrismaResult};
use prisma_models::prelude::*;
use prost::Message;
use std::error::Error as StdError;

pub trait ExternalInterface {
    fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8>;
    fn get_nodes(&self, payload: &mut [u8]) -> Vec<u8>;
    fn get_related_nodes(&self, payload: &mut [u8]) -> Vec<u8>;
    fn get_scalar_list_values(&self, payload: &mut [u8]) -> Vec<u8>;
}

pub struct ProtoBufInterface {
    data_resolver: Box<dyn DataResolver + Send + Sync + 'static>,
}

impl ProtoBufInterface {
    pub fn new(config: &PrismaConfig) -> ProtoBufInterface {
        let data_resolver = match config.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite-native" => {
                SqlResolver::new(Sqlite::new(config.limit(), config.test_mode).unwrap())
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };

        ProtoBufInterface {
            data_resolver: Box::new(data_resolver),
        }
    }

    fn protobuf_result<F>(f: F) -> Vec<u8>
    where
        F: FnOnce() -> PrismaResult<Vec<u8>>,
    {
        f().unwrap_or_else(|error| match error {
            Error::NoResultError => {
                let response = prisma::RpcResponse::empty();
                let mut response_payload = Vec::new();

                response.encode(&mut response_payload).unwrap();
                response_payload
            }
            _ => {
                dbg!(&error);

                let error_response = prisma::RpcResponse::error(error);

                let mut payload = Vec::new();
                error_response.encode(&mut payload).unwrap();
                payload
            }
        })
    }
}

impl InputValidation for GetNodeByWhereInput {
    fn validate(&self) -> PrismaResult<()> {
        Ok(())
    }
}

impl ExternalInterface for ProtoBufInterface {
    fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = GetNodeByWhereInput::decode(payload)?;
            input.validate()?;

            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;
            let project: ProjectRef = project_template.into();

            let model = project.schema().find_model(&input.model_name)?;
            let selected_fields = input.selected_fields.into_selected_fields(model.clone(), None);

            let value: PrismaValue = input.value.into();
            let field = model.fields().find_from_scalar(&input.field_name)?;
            let node_selector = NodeSelector { field, value };

            let query_result = self.data_resolver.get_node_by_where(node_selector, selected_fields)?;

            let (nodes, fields) = match query_result {
                Some(node) => (vec![node.node.into()], node.field_names),
                _ => (Vec::new(), Vec::new()),
            };

            let response = RpcResponse::ok(prisma::NodesResult { nodes, fields });
            let mut response_payload = Vec::new();

            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn get_nodes(&self, _: &mut [u8]) -> Vec<u8> {
        unimplemented!()
    }
    fn get_related_nodes(&self, _: &mut [u8]) -> Vec<u8> {
        unimplemented!()
    }
    fn get_scalar_list_values(&self, _: &mut [u8]) -> Vec<u8> {
        unimplemented!()
    }
}

impl From<Error> for super::prisma::error::Value {
    fn from(error: Error) -> super::prisma::error::Value {
        match error {
            Error::ConnectionError(message, _) => super::prisma::error::Value::ConnectionError(message.to_string()),
            Error::QueryError(message, _) => super::prisma::error::Value::QueryError(message.to_string()),
            Error::ProtobufDecodeError(message, _) => {
                super::prisma::error::Value::ProtobufDecodeError(message.to_string())
            }
            Error::JsonDecodeError(message, _) => super::prisma::error::Value::JsonDecodeError(message.to_string()),
            Error::InvalidInputError(message) => super::prisma::error::Value::InvalidInputError(message.to_string()),
            Error::InvalidConnectionArguments(message) => {
                super::prisma::error::Value::InvalidConnectionArguments(message.to_string())
            }
            e @ Error::NoResultError => super::prisma::error::Value::NoResultsError(e.description().to_string()),
        }
    }
}
