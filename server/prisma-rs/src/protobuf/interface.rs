use crate::{
    config::{ConnectionLimit, PrismaConfig, PrismaDatabase},
    connectors::{PrismaConnector, Sqlite},
    error::Error,
    executor::QueryExecutor,
    protobuf::prisma,
    PrismaResult,
};

use prost::Message;

pub trait ScalaInterface {
    fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8>;
    fn get_nodes(&self, payload: &mut [u8]) -> Vec<u8>;
}

pub struct ProtoBufInterface {
    connector: PrismaConnector,
}

impl ProtoBufInterface {
    pub fn new(config: &PrismaConfig) -> ProtoBufInterface {
        let connector = match config.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite" => {
                Sqlite::new(config.limit(), config.test_mode).unwrap()
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };

        ProtoBufInterface {
            connector: Box::new(connector),
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

impl ScalaInterface for ProtoBufInterface {
    fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = prisma::GetNodeByWhereInput::decode(payload)?;
            let (nodes, fields) = input.query(&self.connector)?;
            let response = prisma::RpcResponse::ok(prisma::NodesResult { nodes, fields });

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn get_nodes(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = prisma::GetNodesInput::decode(payload)?;
            let (nodes, fields) = input.query(&self.connector)?;
            let response = prisma::RpcResponse::ok(prisma::NodesResult { nodes, fields });

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }
}
