use crate::{
    data_resolvers::{IntoSelectQuery, SelectQuery},
    error::Error,
    models::prelude::*,
    protobuf::prelude::*,
    PrismaResult,
};

use prisma_query::ast::*;

impl IntoSelectQuery for GetNodeByWhereInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        let project_template: ProjectTemplate =
            serde_json::from_reader(self.project_json.as_slice())?;

        let project: ProjectRef = project_template.into();
        let model = project.schema().find_model(&self.model_name)?;
        let selected_fields = Self::selected_fields(&model, self.selected_fields);

        let value = self.value.prisma_value.ok_or_else(|| {
            Error::InvalidInputError(String::from("Search value cannot be empty."))
        })?;

        let condition = ConditionTree::single(self.field_name.equals(value));
        let base_query = Self::base_query(model.db_name(), condition, 0);
        let select_ast = Self::select_fields(base_query, &selected_fields);

        dbg!(Ok(SelectQuery {
            db_name: project.db_name().to_string(),
            query_ast: select_ast,
            selected_fields: selected_fields,
        }))
    }
}
