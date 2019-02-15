use crate::{
    cursor_condition::CursorCondition,
    data_resolvers::{IntoSelectQuery, SelectQuery},
    models::prelude::*,
    protobuf::prelude::*,
    PrismaResult,
};

use sql::prelude::*;
use std::collections::BTreeSet;

impl IntoSelectQuery for GetNodesInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        let project_template: ProjectTemplate =
            serde_json::from_reader(self.project_json.as_slice())?;

        let fields = self
            .selected_fields
            .into_iter()
            .fold(BTreeSet::new(), |mut acc, field| {
                if let Some(selected_field::Field::Scalar(s)) = field.field {
                    acc.insert(s);
                };
                acc
            });

        let project: ProjectRef = project_template.into();
        let model = project.schema().find_model(&self.model_name)?;
        let cursor = CursorCondition::build(&self.query_arguments, &model);

        let filter = self
            .query_arguments
            .filter
            .map(|filter| filter.into())
            .unwrap_or(ConditionTree::NoCondition);

        let conditions = ConditionTree::and(filter, cursor);

        let query = SelectQuery {
            project: project,
            model: model,
            selected_fields: fields,
            conditions: conditions,
            order_by: None, // TODO
            skip: self.query_arguments.skip,
            first: self.query_arguments.first,
        };

        Ok(query)
    }
}
