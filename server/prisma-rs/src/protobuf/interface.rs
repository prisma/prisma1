use crate::{
    config::{ConnectionLimit, PrismaConfig, PrismaDatabase},
    connector::{Connector, Sqlite},
    error::Error,
    models::{Project, ProjectTemplate, Renameable, ScalarField},
    protobuf::prisma,
    querying::NodeSelector,
    PrismaResult,
};

use prost::Message;

pub struct ProtoBufInterface {
    connector: Box<dyn Connector + Send + Sync + 'static>,
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

    pub fn get_node_by_where(&self, payload: &mut [u8]) -> Vec<u8> {
        Self::protobuf_result(|| {
            let params = prisma::GetNodeByWhereInput::decode(payload)?;

            let project_template: ProjectTemplate =
                serde_json::from_reader(params.project_json.as_slice())?;

            let project: Project = project_template.into();

            let model = project
                .schema
                .find_model(&params.model_name)
                .ok_or_else(|| {
                    Error::InvalidInputError(format!("Model not found: {}", params.model_name))
                })?;

            let selected_fields: Vec<&ScalarField> = model.find_fields(&params.selected_scalar());

            let field = model.find_field(&params.field_name).ok_or_else(|| {
                Error::InvalidInputError(format!("Field not found: {}", params.field_name))
            })?;

            let value = params.value.prisma_value.ok_or_else(|| {
                Error::InvalidInputError(String::from("Search value cannot be empty."))
            })?;

            let node_selector =
                NodeSelector::new(model.clone(), field, &value, selected_fields.as_slice());

            let result = self
                .connector
                .get_node_by_where(project.db_name(), &node_selector)?;

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

            let response = prisma::RpcResponse::ok(prisma::NodesResult { nodes, fields });

            let mut response_payload = Vec::new();
            response.encode(&mut response_payload).unwrap();

            Ok(response_payload)
        })
    }
}
