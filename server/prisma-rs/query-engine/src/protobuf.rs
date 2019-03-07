mod envelope;
mod filter;
mod input;
mod interface;

pub mod prelude;

use chrono::prelude::*;
pub use envelope::ProtoBufEnvelope;
pub use filter::*;
pub use input::*;
pub use interface::{ExternalInterface, ProtoBufInterface};

use crate::Error as CrateError;
use prelude::*;
use prisma_common::{error::Error, PrismaResult};
use prisma_models::prelude::*;
use prisma_query::ast::*;
use std::sync::Arc;

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

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

    pub fn ok(result: prisma::NodesResult) -> RpcResponse {
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

impl From<prisma::order_by::SortOrder> for Order {
    fn from(so: prisma::order_by::SortOrder) -> Order {
        match so {
            prisma::order_by::SortOrder::Asc => Order::Asc,
            prisma::order_by::SortOrder::Desc => Order::Desc,
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
            vc::PrismaValue::DateTime(v) => PrismaValue::DateTime(v.parse::<DateTime<Utc>>().unwrap()),
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
            id::IdValue::Uuid(s) => GraphqlId::String(s),
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
            PrismaValue::DateTime(v) => vc::PrismaValue::DateTime(v.to_rfc3339()),
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
            GraphqlId::UUID(s) => id::IdValue::Uuid(s),
        };

        prisma::GraphqlId {
            id_value: Some(id_value),
        }
    }
}

impl From<Node> for prisma::Node {
    fn from(node: Node) -> prisma::Node {
        prisma::Node {
            values: node.values.into_iter().map(ValueContainer::from).collect(),
            parent_id: node.parent_id.map(prisma::GraphqlId::from),
        }
    }
}

impl IntoSelectedFields for prisma::SelectedFields {
    fn into_selected_fields(self, model: ModelRef, from_field: Option<Arc<RelationField>>) -> SelectedFields {
        let fields = self.fields.into_iter().fold(Vec::new(), |mut acc, sf| {
            match sf.field.unwrap() {
                prisma::selected_field::Field::Scalar(field_name) => {
                    let field = model.fields().find_from_scalar(&field_name).unwrap();

                    acc.push(SelectedField::Scalar(SelectedScalarField { field }));
                }
                prisma::selected_field::Field::Relational(rf) => {
                    let field = model.fields().find_from_relation_fields(&rf.field).unwrap();

                    let selected_fields = rf
                        .selected_fields
                        .into_selected_fields(model.clone(), from_field.clone());

                    acc.push(SelectedField::Relation(SelectedRelationField {
                        field,
                        selected_fields,
                    }));
                }
            }

            acc
        });

        SelectedFields::new(fields, from_field)
    }
}

impl crate::protobuf::QueryArguments {
    pub fn is_with_pagination(&self) -> bool {
        self.last.or(self.first).or(self.skip).is_some()
    }

    pub fn window_limits(&self) -> (u32, u32) {
        let skip = self.skip.unwrap_or(0) + 1;

        match self.last.or(self.first) {
            Some(limited_count) => (skip, limited_count + skip),
            None => (skip, 100000000),
        }
    }
}

impl IntoOrderBy for prisma::OrderBy {
    fn into_order_by(self, model: ModelRef) -> OrderBy {
        let field = model.fields().find_from_scalar(&self.scalar_field).unwrap();

        let sort_order = match self.sort_order() {
            prisma::order_by::SortOrder::Asc => SortOrder::Ascending,
            prisma::order_by::SortOrder::Desc => SortOrder::Descending,
        };

        OrderBy { field, sort_order }
    }
}

trait InputValidation {
    fn validate(&self) -> PrismaResult<()>;

    fn validate_args(query_arguments: &crate::protobuf::QueryArguments) -> PrismaResult<()> {
        if let (Some(_), Some(_)) = (query_arguments.first, query_arguments.last) {
            return Err(Error::InvalidConnectionArguments(
                "Cannot have first and last set in the same query",
            ));
        };

        Ok(())
    }
}
