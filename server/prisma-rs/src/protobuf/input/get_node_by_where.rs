use crate::{
    data_resolvers::{IntoSelectQuery, SelectQuery},
    error::Error,
    models::prelude::*,
    protobuf::prelude::*,
    PrismaResult,
};

use sql::prelude::*;
use std::collections::BTreeSet;

impl IntoSelectQuery for GetNodeByWhereInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        let project_template: ProjectTemplate =
            serde_json::from_reader(self.project_json.as_slice())?;

        let project: ProjectRef = project_template.into();
        let model = project.schema().find_model(&self.model_name)?;

        let fields = self
            .selected_fields
            .into_iter()
            .fold(BTreeSet::new(), |mut acc, field| {
                if let Some(selected_field::Field::Scalar(s)) = field.field {
                    acc.insert(s);
                };
                acc
            });

        let value = self.value.prisma_value.ok_or_else(|| {
            Error::InvalidInputError(String::from("Search value cannot be empty."))
        })?;

        let condition = ConditionTree::single(column(&self.field_name).equals(value));

        let query = SelectQuery {
            project: project,
            model: model,
            selected_fields: fields,
            conditions: condition,
            order_by: None, // TODO
            skip: 0,
            limit: None,
        };

        Ok(query)
    }
}
