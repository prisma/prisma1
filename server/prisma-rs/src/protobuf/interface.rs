use crate::{
    config::{ConnectionLimit, PrismaConfig, PrismaDatabase},
    data_resolvers::{IntoSelectQuery, PrismaDataResolver, Sqlite},
    error::Error,
    protobuf::prelude::*,
    PrismaResult,
};

use prost::Message;

macro_rules! input_to_query {
    ( $x:tt, $y:ident ) => {
        fn $y(&self, payload: &mut [u8]) -> Vec<u8> {
            Self::protobuf_result(|| {
                let input = $x::decode(payload)?;
                let query = input.into_select_query()?;
                let (nodes, fields) = self.data_resolver.select_nodes(query)?;
                let response = RpcResponse::ok(NodesResult { nodes, fields });

                let mut response_payload = Vec::new();
                response.encode(&mut response_payload).unwrap();

                Ok(response_payload)
            })
        }
    }
}

pub trait ExternalInterface {
    fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8>;
    fn get_nodes(&self, payload: &mut [u8]) -> Vec<u8>;
    fn get_related_nodes(&self, payload: &mut [u8]) -> Vec<u8>;
}

pub struct ProtoBufInterface {
    data_resolver: PrismaDataResolver,
}

impl ProtoBufInterface {
    pub fn new(config: &PrismaConfig) -> ProtoBufInterface {
        let data_resolver = match config.databases.get("default") {
            Some(PrismaDatabase::File(ref config)) if config.connector == "sqlite" => {
                Sqlite::new(config.limit(), config.test_mode).unwrap()
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

    fn validate(query_arguments: &QueryArguments) -> PrismaResult<()> {
        if let (Some(_), Some(_)) = (query_arguments.first, query_arguments.last) {
            return Err(Error::InvalidConnectionArguments(
                "Cannot have first and last set in the same query",
            ));
        };

        Ok(())
    }
}

impl ExternalInterface for ProtoBufInterface {
    input_to_query!(GetNodeByWhereInput, get_node_by_where);
    input_to_query!(GetNodesInput, get_nodes);
    input_to_query!(GetRelatedNodesInput, get_related_nodes);
}
