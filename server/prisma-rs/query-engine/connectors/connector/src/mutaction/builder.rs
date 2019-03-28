#[allow(unused_imports)]
use itertools::Itertools;
use prisma_models::prelude::*;
use prisma_query::ast::*;

use crate::{error::ConnectorError, ConnectorResult};

pub struct MutationBuilder;

impl MutationBuilder {
    const PARAMETER_LIMIT: usize = 10000;

    pub fn create_node(model: ModelRef, mut args: PrismaArgs) -> (Insert, Option<GraphqlId>) {
        let model_id = model.fields().id();

        let return_id = match args.get_field_value(&model_id.name) {
            _ if model_id.is_auto_generated => None,
            Some(PrismaValue::Null) | None => {
                let id = model.generate_id();
                args.insert(model_id.name.as_ref(), id.clone());
                Some(id)
            }
            Some(PrismaValue::GraphqlId(id)) => Some(id.clone()),
            _ => None,
        };

        let fields: Vec<&Field> = model
            .fields()
            .all
            .iter()
            .filter(|field| args.has_arg_for(&field.name()))
            .collect();

        let fields = fields
            .iter()
            .map(|field| (field.name(), args.take_field_value(field.name()).unwrap()));

        let base = Insert::single_into(model.table());

        let insert = fields
            .into_iter()
            .fold(base, |acc, (name, value)| acc.value(name, value));

        (insert.into(), return_id)
    }

    pub fn create_scalar_list_value(
        scalar_list_table: Table,
        list_value: &PrismaListValue,
        id: &GraphqlId,
    ) -> Option<Insert> {
        if list_value.is_empty() {
            return None;
        }

        let positions = (1..=list_value.len()).map(|v| (v * 1000) as i64);
        let values = list_value.iter().zip(positions);

        let columns = vec![
            ScalarListTable::POSITION_FIELD_NAME,
            ScalarListTable::VALUE_FIELD_NAME,
            ScalarListTable::NODE_ID_FIELD_NAME,
        ];

        let insert = Insert::multi_into(scalar_list_table, columns);

        let result = values
            .fold(insert, |acc, (value, position)| {
                acc.values((position, value.clone(), id.clone()))
            })
            .into();

        Some(result)
    }

    pub fn update_node_by_id(model: ModelRef, id: GraphqlId, args: &PrismaArgs) -> ConnectorResult<Option<Update>> {
        Self::update_by_ids(model, args, vec![id]).map(|updates| updates.into_iter().next())
    }

    pub fn update_by_ids(model: ModelRef, args: &PrismaArgs, ids: Vec<GraphqlId>) -> ConnectorResult<Vec<Update>> {
        if args.args.is_empty() || ids.is_empty() {
            return Ok(Vec::new());
        }

        let fields = model.fields();
        let mut query = Update::table(model.table());

        for (name, value) in args.args.iter() {
            let field = fields.find_from_scalar(&name).unwrap();

            if field.is_required && value.is_null() {
                return Err(ConnectorError::FieldCannotBeNull {
                    field: field.name.clone(),
                });
            }

            query = query.set(field.db_name(), value.clone());
        }

        let result: Vec<Update> = ids
            .chunks(Self::PARAMETER_LIMIT)
            .into_iter()
            .map(|ids| {
                query
                    .clone()
                    .so_that(fields.id().as_column().in_selection(ids.to_vec()))
            })
            .collect();

        Ok(result)
    }

    pub fn update_scalar_list_value_by_ids(
        scalar_list_table: ScalarListTable,
        list_value: &PrismaListValue,
        ids: Vec<GraphqlId>,
    ) -> (Vec<Delete>, Vec<Insert>) {
        if ids.is_empty() {
            return (Vec::new(), Vec::new());
        }

        let deletes = ids
            .chunks(Self::PARAMETER_LIMIT)
            .into_iter()
            .map(|ids| {
                Delete::from(scalar_list_table.table())
                    .so_that(ScalarListTable::NODE_ID_FIELD_NAME.in_selection(ids.to_vec()))
            })
            .collect();

        let inserts = if list_value.is_empty() {
            Vec::new()
        } else {
            ids.iter()
                .flat_map(|id| Self::create_scalar_list_value(scalar_list_table.table(), list_value, id))
                .collect()
        };

        (deletes, inserts)
    }
}
