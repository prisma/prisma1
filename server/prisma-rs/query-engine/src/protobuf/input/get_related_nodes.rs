use crate::{
    data_resolvers::{IntoSelectQuery, SelectQuery},
    protobuf::{prelude::*, InputValidation},
    related_nodes_query_builder::RelatedNodesQueryBuilder,
};

use prisma_common::PrismaResult;
use prisma_models::prelude::*;

impl IntoSelectQuery for GetRelatedNodesInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        let project_template: ProjectTemplate = serde_json::from_reader(self.project_json.as_slice())?;

        let project: ProjectRef = project_template.into();
        let model = project.schema().find_model(&self.model_name)?;

        let from_field = model.fields().find_from_relation_fields(&self.from_field)?;
        let from_node_ids: Vec<GraphqlId> = self.from_node_ids.into_iter().map(GraphqlId::from).collect();
        let is_with_pagination = self.query_arguments.is_with_pagination();
        let selected_fields = self.selected_fields.into_selected_fields(model.clone(), None);

        let builder = RelatedNodesQueryBuilder::new(from_field, from_node_ids, self.query_arguments, &selected_fields);

        let select_ast = if is_with_pagination {
            builder.with_pagination()
        } else {
            builder.without_pagination()
        };

        Ok(SelectQuery {
            db_name: project.schema().db_name.to_string(),
            query_ast: select_ast,
            selected_fields: selected_fields,
        })
    }
}

impl InputValidation for GetRelatedNodesInput {
    fn validate(&self) -> PrismaResult<()> {
        Self::validate_args(&self.query_arguments)
    }
}
