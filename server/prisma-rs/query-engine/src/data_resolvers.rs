mod sqlite;

pub use sqlite::Sqlite;
pub type PrismaDataResolver = Box<dyn DataResolver + Send + Sync + 'static>;

use crate::{ordering::OrderVec, protobuf::prelude::*};
use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::ast::*;
use std::{collections::BTreeSet, sync::Arc};

#[derive(Debug, Default)]
pub struct SelectedFields {
    pub fields: Vec<Arc<ScalarField>>,
}

impl SelectedFields {
    fn names(&self) -> Vec<String> {
        self.fields.iter().map(|f| f.db_name().to_string()).collect()
    }

    fn names_of_scalar_non_list_fields(&self) -> Vec<String> {
        self.fields
            .iter()
            .filter(|f| !f.is_list)
            .map(|f| f.db_name().to_string())
            .collect()
    }
}

#[derive(Debug)]
pub struct SelectQuery {
    pub db_name: String,
    pub query_ast: Select,
    pub selected_fields: SelectedFields,
}

pub trait IntoSelectQuery {
    fn into_select_query(self) -> PrismaResult<SelectQuery>;

    fn selected_fields(model: &Model, fields: Vec<SelectedField>) -> SelectedFields {
        let fields = fields.into_iter().fold(BTreeSet::new(), |mut acc, field| {
            if let Some(selected_field::Field::Scalar(s)) = field.field {
                acc.insert(s);
            };
            acc
        });

        let fields = model.fields().find_many_from_scalar(&fields);

        SelectedFields { fields }
    }

    fn base_query(table: &str, conditions: ConditionTree, offset: usize) -> Select {
        Select::from(table).so_that(conditions).offset(offset)
    }

    fn select_fields(select: Select, fields: &SelectedFields) -> Select {
        fields
            .names_of_scalar_non_list_fields()
            .iter()
            .fold(select, |acc, field| acc.column(field.as_ref()))
    }

    fn order_by(select: Select, ordering: OrderVec) -> Select {
        ordering
            .into_iter()
            .fold(select, |acc, order_by| acc.order_by(order_by))
    }

    fn limit(select: Select, limit: Option<usize>) -> Select {
        if let Some(limit) = limit {
            select.limit(limit)
        } else {
            select
        }
    }
}

pub trait DataResolver {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<(Vec<Vec<PrismaValue>>, Vec<String>)>;
}
