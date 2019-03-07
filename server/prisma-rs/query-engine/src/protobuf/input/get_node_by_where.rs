use crate::{
    data_resolvers::{IntoSelectQuery, SelectQuery},
    protobuf::{prelude::*, InputValidation},
};
use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::ast::*;

impl IntoSelectQuery for GetNodeByWhereInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        let project_template: ProjectTemplate = serde_json::from_reader(self.project_json.as_slice())?;

        let project: ProjectRef = project_template.into();
        let model = project.schema().find_model(&self.model_name)?;
        let selected_fields = self.selected_fields.into_selected_fields(model.clone(), None);
        let value: PrismaValue = self.value.into();
        let field = model.fields().find_from_scalar(&self.field_name)?;
        let condition = ConditionTree::single(field.as_column().equals(value));

        let base_query = Select::from(model.table()).so_that(condition).offset(0);

        let select_ast = selected_fields
            .columns()
            .into_iter()
            .fold(base_query, |acc, column| acc.column(column.clone()));

        Ok(SelectQuery {
            db_name: project.schema().db_name.to_string(),
            query_ast: select_ast,
            selected_fields: selected_fields,
        })
    }
}

impl InputValidation for GetNodeByWhereInput {
    fn validate(&self) -> PrismaResult<()> {
        Ok(())
    }
}
