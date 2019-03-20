use super::query_arguments::into_model_query_arguments;
use crate::{
    protobuf::{prelude::*, InputValidation},
    ExternalInterface,
};
use connector::*;
use prisma_common::{config::WithMigrations, config::*, error::Error, PrismaResult};
use prisma_models::prelude::*;
use prost::Message;
use sqlite_connector::{SqlResolver, Sqlite, SqliteDatabaseMutactionExecutor};
use std::{error::Error as StdError, sync::Arc};

pub struct ProtoBufInterface {
    data_resolver: Box<dyn DataResolver + Send + Sync + 'static>,
    database_mutaction_executor: Box<dyn DatabaseMutactionExecutor + Send + Sync + 'static>,
}

impl ProtoBufInterface {
    pub fn new(config: &PrismaConfig) -> ProtoBufInterface {
        let (data_resolver, mutaction_executor) = match config.databases.get("default") {
            Some(PrismaDatabase::Explicit(ref config)) if config.connector == "sqlite-native" => {
                let sqlite = Arc::new(Sqlite::new(config.limit(), config.is_active().unwrap()).unwrap());

                (
                    SqlResolver::new(sqlite.clone()),
                    SqliteDatabaseMutactionExecutor { _sqlite: sqlite },
                )
            }
            _ => panic!("Database connector is not supported, use sqlite with a file for now!"),
        };

        ProtoBufInterface {
            data_resolver: Box::new(data_resolver),
            database_mutaction_executor: Box::new(mutaction_executor),
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

            let query_result = self.data_resolver.get_node_by_where(&node_selector, &selected_fields)?;

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

    fn get_nodes(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = GetNodesInput::decode(payload)?;
            input.validate()?;

            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;
            let project: ProjectRef = project_template.into();

            let model = project.schema().find_model(&input.model_name)?;
            let selected_fields = input.selected_fields.into_selected_fields(model.clone(), None);
            let query_arguments = into_model_query_arguments(model.clone(), input.query_arguments);

            let query_result = self.data_resolver.get_nodes(model, query_arguments, selected_fields)?;
            let (nodes, fields) = (query_result.nodes, query_result.field_names);
            let proto_nodes = nodes.into_iter().map(|n| n.into()).collect();

            let response = RpcResponse::ok(prisma::NodesResult {
                nodes: proto_nodes,
                fields: fields,
            });

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn get_related_nodes(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = GetRelatedNodesInput::decode(payload)?;
            input.validate()?;

            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;

            let project: ProjectRef = project_template.into();
            let model = project.schema().find_model(&input.model_name)?;

            let from_field = model.fields().find_from_relation_fields(&input.from_field)?;
            let from_node_ids: Vec<GraphqlId> = input.from_node_ids.into_iter().map(GraphqlId::from).collect();
            let related_model = from_field.related_model();

            let selected_fields = input
                .selected_fields
                .into_selected_fields(Arc::clone(&related_model), Some(from_field.clone()));

            let query_result = self.data_resolver.get_related_nodes(
                from_field,
                &from_node_ids,
                into_model_query_arguments(Arc::clone(&related_model), input.query_arguments),
                &selected_fields,
            )?;

            let (nodes, fields) = (query_result.nodes, query_result.field_names);
            let proto_nodes = nodes.into_iter().map(|n| n.into()).collect();

            let response = RpcResponse::ok(prisma::NodesResult {
                nodes: proto_nodes,
                fields: fields,
            });

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn get_scalar_list_values_by_node_ids(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = GetScalarListValuesByNodeIds::decode(payload)?;
            input.validate()?;

            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;
            let project: ProjectRef = project_template.into();

            let model = project.schema().find_model(&input.model_name)?;
            let list_field = model.fields().find_from_scalar(&input.list_field)?;

            let node_ids: Vec<GraphqlId> = input.node_ids.into_iter().map(GraphqlId::from).collect();

            let query_result = self
                .data_resolver
                .get_scalar_list_values_by_node_ids(list_field, node_ids)?;

            let proto_values = query_result
                .into_iter()
                .map(|vals| prisma::ScalarListValues {
                    node_id: vals.node_id.into(),
                    values: vals.values.into_iter().map(|n| n.into()).collect(),
                })
                .collect();

            let response = RpcResponse::ok(prisma::ScalarListValuesResult { values: proto_values });

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn count_by_model(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = CountByModelInput::decode(payload)?;
            input.validate()?;

            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;
            let project: ProjectRef = project_template.into();
            let model = project.schema().find_model(&input.model_name)?;

            let query_arguments = into_model_query_arguments(model.clone(), input.query_arguments);
            let count = self.data_resolver.count_by_model(model, query_arguments)?;

            let response = RpcResponse::ok(count);

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn count_by_table(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = CountByTableInput::decode(payload)?;
            input.validate()?;

            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;
            let project: ProjectRef = project_template.into();

            let count = match project.schema().find_model(&input.model_name) {
                Ok(model) => self
                    .data_resolver
                    .count_by_table(project.schema().db_name.as_ref(), model.db_name()),
                Err(_) => self
                    .data_resolver
                    .count_by_table(project.schema().db_name.as_ref(), &input.model_name),
            }?;

            let response = RpcResponse::ok(count);

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn execute_raw(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = ExecuteRawInput::decode(payload)?;
            let json = self.database_mutaction_executor.execute_raw(input.query);
            let json_as_string = serde_json::to_string(&json)?;

            let response = RpcResponse::ok_raw(prisma::ExecuteRawResult { json: json_as_string });
            let mut response_payload = Vec::new();

            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }

    fn execute_mutaction(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let input = crate::protobuf::prisma::DatabaseMutaction::decode(payload)?;
            let project_template: ProjectTemplate = serde_json::from_reader(input.project_json.as_slice())?;
            let project: ProjectRef = project_template.into();
            let model = project.schema().find_model(&input.model_name).unwrap();
            let mutaction = convert_mutaction(input, model);

            let mut results = self.database_mutaction_executor.execute(mutaction)?;
            let result = results.results.pop().expect("no mutaction results returned");

            let response = RpcResponse::ok_mutaction(convert_mutaction_result(result));
            let mut response_payload = Vec::new();

            response.encode(&mut response_payload).unwrap();
            Ok(response_payload)
        })
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
            Error::InvalidInputError(message, _) => super::prisma::error::Value::InvalidInputError(message.to_string()),
            Error::InvalidConnectionArguments(message) => {
                super::prisma::error::Value::InvalidConnectionArguments(message.to_string())
            }
            e @ Error::NoResultError => super::prisma::error::Value::NoResultsError(e.description().to_string()),
            _ => unreachable!(),
        }
    }
}

fn convert_mutaction(m: crate::protobuf::prisma::DatabaseMutaction, model: ModelRef) -> DatabaseMutaction {
    use crate::protobuf::prisma::database_mutaction;
    let m = match m.type_.unwrap() {
        database_mutaction::Type::Create(x) => convert_create(x, model),
    };

    DatabaseMutaction::TopLevel(m)
}

fn convert_create(m: crate::protobuf::prisma::CreateNode, model: ModelRef) -> TopLevelDatabaseMutaction {
    let create_node = CreateNode {
        model: model,
        non_list_args: convert_prisma_args(m.non_list_args),
        list_args: convert_prisma_args(m.list_args),
        nested_mutactions: empty_nested_mutactions(),
    };
    TopLevelDatabaseMutaction::CreateNode(create_node)
}

fn empty_nested_mutactions() -> NestedMutactions {
    NestedMutactions { creates: vec![] }
}

fn convert_prisma_args(proto: crate::protobuf::prisma::PrismaArgs) -> PrismaArgs {
    let mut result = PrismaArgs::empty();
    for arg in proto.args {
        result.insert(arg.key, arg.value);
    }
    result
}

fn convert_mutaction_result(result: DatabaseMutactionResult) -> crate::protobuf::prisma::DatabaseMutactionResult {
    use crate::protobuf::prisma::database_mutaction_result;
    match result {
        DatabaseMutactionResult::CreateNode(x) => {
            let result = crate::protobuf::prisma::CreateNodeResult { id: x.id.into() };
            let type_ = database_mutaction_result::Type::Create(result);
            crate::protobuf::prisma::DatabaseMutactionResult { type_: Some(type_) }
        }
    }
}
