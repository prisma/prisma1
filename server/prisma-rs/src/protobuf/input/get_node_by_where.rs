use crate::{
    connectors::{IntoSelectQuery, SelectQuery},
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

        let project: Project = project_template.into();

        let fields = self
            .selected_fields
            .into_iter()
            .fold(BTreeSet::new(), |mut acc, field| {
                if let Some(selected_field::Field::Scalar(s)) = field.field {
                    acc.insert(s);
                };
                acc
            });

        let field = &self.field_name;
        let value = self.value.prisma_value.ok_or_else(|| {
            Error::InvalidInputError(String::from("Search value cannot be empty."))
        })?;

        let query = SelectQuery {
            project: project,
            model_name: self.model_name,
            selected_fields: fields,
            conditions: ConditionTree::single(field.equals(value)),
            order_by: None, // TODO
            skip: None,
            after: None,
            first: None,
        };

        Ok(query)
    }
}
