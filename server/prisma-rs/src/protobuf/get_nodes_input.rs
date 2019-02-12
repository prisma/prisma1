use crate::{
    connector::PrismaConnector,
    executor::QueryExecutor,
    models::{Project, ProjectTemplate, Renameable},
    protobuf::prisma::{selected_field, GetNodesInput, Node},
    PrismaResult,
};

use std::collections::BTreeSet;

// TODO: This is wrong
impl GetNodesInput {
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

impl QueryExecutor for GetNodesInput {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<Node>, Vec<String>)> {
        let project_template: ProjectTemplate =
            serde_json::from_reader(self.project_json.as_slice())?;
        let project: Project = project_template.into();
        let model = project.schema.find_model(&self.model_name)?;

        let selected_fields = model
            .fields()
            .find_many_from_scalar(&self.selected_fields());

        let nodes = connector.get_nodes(
            project.db_name(),
            &model,
            &selected_fields,
            self.query_arguments,
        )?;

        let fields = Vec::new();

        Ok((nodes, fields))
    }
}
