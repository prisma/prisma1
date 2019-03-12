use crate::{
    cursor_condition::CursorCondition,
    database_executor::{IntoSelectQuery, SelectQuery},
    ordering::Ordering,
    protobuf::filter::IntoFilter,
    protobuf::{prelude::*, InputValidation},
};

use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::ast::*;

impl IntoSelectQuery for GetNodesInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        let project_template: ProjectTemplate = serde_json::from_reader(self.project_json.as_slice())?;

        let project: ProjectRef = project_template.into();
        let model = project.schema().find_model(&self.model_name)?;
        let selected_fields = self.selected_fields.into_selected_fields(model.clone(), None);
        let cursor: ConditionTree = CursorCondition::build(&self.query_arguments, &model);

        let order_by = self
            .query_arguments
            .order_by
            .map(|oby| oby.into_order_by(model.clone()));

        let ordering = Ordering::for_model(&model, order_by.as_ref(), self.query_arguments.last.is_some());

        let filter: ConditionTree = self
            .query_arguments
            .filter
            .map(|filter| filter.into_filter(model.clone()))
            .map(|filter| filter.into())
            .unwrap_or(ConditionTree::NoCondition);

        let conditions = ConditionTree::and(filter, cursor);

        let (skip, limit) = match self.query_arguments.last.or(self.query_arguments.first) {
            Some(c) => (self.query_arguments.skip.unwrap_or(0), Some(c + 1)), // +1 to see if there's more data
            None => (self.query_arguments.skip.unwrap_or(0), None),
        };

        let select_ast = Select::from(model.table()).so_that(conditions).offset(skip as usize);
        let select_ast = ordering.into_iter().fold(select_ast, |acc, ord| acc.order_by(ord));

        let select_ast = selected_fields
            .columns()
            .into_iter()
            .fold(select_ast, |acc, col| acc.column(col.clone()));

        let select_ast = match limit {
            Some(limit) => select_ast.limit(limit as usize),
            None => select_ast,
        };

        Ok(SelectQuery {
            db_name: project.schema().db_name.to_string(),
            query_ast: select_ast,
            selected_fields: selected_fields,
        })
    }
}

impl InputValidation for GetNodesInput {
    fn validate(&self) -> PrismaResult<()> {
        Self::validate_args(&self.query_arguments)
    }
}
