use crate::{
    connector::PrismaConnector,
    executor::QueryExecutor,
    models::{Project, ProjectTemplate, Renameable},
    protobuf::prisma::{GetNodesInput, Node},
    PrismaResult,
};

impl QueryExecutor for GetNodesInput {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<Node>, Vec<String>)> {
        let project_template: ProjectTemplate =
            serde_json::from_reader(self.project_json.as_slice())?;
        let project: Project = project_template.into();
        let model = project.schema.find_model(&self.model_name)?;

        let nodes = connector.get_nodes(
            project.db_name(),
            model.db_name(),
            &[],
            &self.query_arguments,
        )?;

        let fields = Vec::new();

        Ok((nodes, fields))
    }
}
