mod envelope;
mod interface;

pub use envelope::ProtoBufEnvelope;
pub use interface::ProtoBufInterface;

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

use prisma::{
    RpcResponse,
    rpc_response as rpc,
    result,
    Header,
    Result,
    Error as ProtoError,
    NodesResult,
};

use crate::{Error as CrateError};

impl RpcResponse {
    pub fn header() -> Header {
        Header {
            type_name: String::from("RpcResponse")
        }
    }

    pub fn empty() -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(Result { value: None })),
        }
    }

    pub fn ok(result: NodesResult) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(
                prisma::Result {
                    value: Some(result::Value::NodesResult(result)),
                }
            ))
        }
    }

    pub fn error(error: CrateError) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Error(ProtoError {
                value: Some(error.into())
            })),
        }
    }
}
