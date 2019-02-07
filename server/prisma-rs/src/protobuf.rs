mod envelope;
mod interface;

pub use envelope::ProtoBufEnvelope;
pub use interface::ProtoBufInterface;

use std::collections::BTreeSet;

use crate::{
    connector::PrismaConnector,
    error::Error,
    models::{Project, ProjectTemplate, Renameable},
    PrismaResult,
};

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

use prisma::{
    result, rpc_response as rpc, selected_field, Error as ProtoError, GetNodeByWhereInput, Header,
    NodesResult, Result, RpcResponse, SelectedField,
};

use crate::Error as CrateError;

impl RpcResponse {
    pub fn header() -> Header {
        Header {
            type_name: String::from("RpcResponse"),
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

pub trait QueryExecutor {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<prisma::Node>, Vec<String>)>;
}

impl QueryExecutor for GetNodeByWhereInput {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<prisma::Node>, Vec<String>)> {
        let project_template: ProjectTemplate =
            serde_json::from_reader(self.project_json.as_slice())?;
        let project: Project = project_template.into();
        let model = project.schema.find_model(&self.model_name)?;
        let selected_fields = model
            .fields()
            .find_many_from_scalar(&self.selected_fields());
        let field = model.fields().find_from_scalar(&self.field_name)?;

        let value = self.value.prisma_value.ok_or_else(|| {
            Error::InvalidInputError(String::from("Search value cannot be empty."))
        })?;

        let result = connector.get_node_by_where(
            project.db_name(),
            model.db_name(),
            &selected_fields,
            (field, &value),
        )?;

        let response_values: Vec<prisma::ValueContainer> = result
            .into_iter()
            .map(|value| prisma::ValueContainer {
                prisma_value: Some(value),
            })
            .collect();

        let nodes = vec![prisma::Node {
            values: response_values,
        }];

        let fields: Vec<String> = selected_fields
            .into_iter()
            .map(|field| field.db_name().to_string())
            .collect();

        Ok((nodes, fields))
    }
}

impl GetNodeByWhereInput {
    pub fn selected_fields(&self) -> BTreeSet<&str> {
        self.selected_fields
            .iter()
            .fold(BTreeSet::new(), |mut acc, field| {
                if let Some(selected_field::Field::Scalar(ref s)) = field.field {
                    acc.insert(s);
                };

                acc
            })
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
