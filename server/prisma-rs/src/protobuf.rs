mod envelope;
mod filter;
mod input;
mod interface;

pub mod prelude;

pub use envelope::ProtoBufEnvelope;
pub use filter::*;
pub use input::*;
pub use interface::{ExternalInterface, ProtoBufInterface};
use prisma_models::prelude::*;
use prisma_query::ast::*;

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

use crate::Error as CrateError;
use prelude::*;

impl RpcResponse {
    pub fn header() -> Header {
        Header {
            type_name: String::from("RpcResponse"),
        }
    }

    pub fn empty() -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(prisma::Result { value: None })),
        }
    }

    pub fn ok(result: NodesResult) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(prisma::Result {
                value: Some(result::Value::NodesResult(result)),
            })),
        }
    }

    pub fn error(error: CrateError) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Error(ProtoError {
                value: Some(error.into()),
            })),
        }
    }
}

impl SelectedField {
    pub fn is_scalar(&self) -> bool {
        match self.field {
            Some(selected_field::Field::Scalar(_)) => true,
            _ => false,
        }
    }

    pub fn is_relational(&self) -> bool {
        match self.field {
            Some(selected_field::Field::Relational { .. }) => true,
            _ => false,
        }
    }
}

impl From<SortOrder> for Order {
    fn from(so: SortOrder) -> Order {
        match so {
            SortOrder::Asc => Order::Asc,
            SortOrder::Desc => Order::Desc,
        }
    }
}

impl From<ValueContainer> for PrismaValue {
    fn from(container: ValueContainer) -> PrismaValue {
        use prisma::value_container as vc;

        match container.prisma_value.unwrap() {
            vc::PrismaValue::String(v) => PrismaValue::String(v),
            vc::PrismaValue::Float(v) => PrismaValue::Float(v),
            vc::PrismaValue::Boolean(v) => PrismaValue::Boolean(v),
            vc::PrismaValue::DateTime(v) => PrismaValue::DateTime(v),
            vc::PrismaValue::Enum(v) => PrismaValue::Enum(v),
            vc::PrismaValue::Json(v) => PrismaValue::Json(v),
            vc::PrismaValue::Int(v) => PrismaValue::Int(v),
            vc::PrismaValue::Relation(v) => PrismaValue::Relation(v as usize),
            vc::PrismaValue::Null(_) => PrismaValue::Null,
            vc::PrismaValue::Uuid(v) => PrismaValue::Uuid(v),
            vc::PrismaValue::GraphqlId(v) => PrismaValue::GraphqlId(v.into()),
        }
    }
}

impl From<prisma::GraphqlId> for GraphqlId {
    fn from(id: prisma::GraphqlId) -> GraphqlId {
        use prisma::graphql_id as id;

        match id.id_value.unwrap() {
            id::IdValue::String(s) => GraphqlId::String(s),
            id::IdValue::Int(i) => GraphqlId::Int(i as usize),
        }
    }
}

impl From<PrismaValue> for ValueContainer {
    fn from(pv: PrismaValue) -> ValueContainer {
        use prisma::value_container as vc;

        let prisma_value = match pv {
            PrismaValue::String(v) => vc::PrismaValue::String(v),
            PrismaValue::Float(v) => vc::PrismaValue::Float(v),
            PrismaValue::Boolean(v) => vc::PrismaValue::Boolean(v),
            PrismaValue::DateTime(v) => vc::PrismaValue::DateTime(v),
            PrismaValue::Enum(v) => vc::PrismaValue::Enum(v),
            PrismaValue::Json(v) => vc::PrismaValue::Json(v),
            PrismaValue::Int(v) => vc::PrismaValue::Int(v),
            PrismaValue::Relation(v) => vc::PrismaValue::Relation(v as i64),
            PrismaValue::Null => vc::PrismaValue::Null(true),
            PrismaValue::Uuid(v) => vc::PrismaValue::Uuid(v),
            PrismaValue::GraphqlId(v) => vc::PrismaValue::GraphqlId(v.into()),
        };

        ValueContainer {
            prisma_value: Some(prisma_value),
        }
    }
}

impl From<GraphqlId> for prisma::GraphqlId {
    fn from(id: GraphqlId) -> prisma::GraphqlId {
        use prisma::graphql_id as id;

        let id_value = match id {
            GraphqlId::String(s) => id::IdValue::String(s),
            GraphqlId::Int(i) => id::IdValue::Int(i as i64),
        };

        prisma::GraphqlId {
            id_value: Some(id_value),
        }
    }
}

impl From<Vec<PrismaValue>> for Node {
    fn from(values: Vec<PrismaValue>) -> Node {
        Node {
            values: values.into_iter().map(ValueContainer::from).collect(),
        }
    }
}
