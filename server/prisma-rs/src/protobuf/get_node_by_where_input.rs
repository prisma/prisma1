use crate::{
    connector::PrismaConnector,
    error::Error,
    executor::QueryExecutor,
    models::{Project, ProjectTemplate, Renameable},
    protobuf::prisma::{selected_field, GetNodeByWhereInput, Node, ValueContainer},
    PrismaResult,
};

use std::collections::BTreeSet;

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

impl QueryExecutor for GetNodeByWhereInput {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<Node>, Vec<String>)> {
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

        let response_values: Vec<ValueContainer> = result
            .into_iter()
            .map(|value| ValueContainer {
                prisma_value: Some(value),
            })
            .collect();

        let nodes = vec![Node {
            values: response_values,
        }];

        let fields: Vec<String> = selected_fields
            .into_iter()
            .map(|field| field.db_name().to_string())
            .collect();

        Ok((nodes, fields))
    }
}
