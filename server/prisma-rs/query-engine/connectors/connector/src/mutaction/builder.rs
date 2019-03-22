use prisma_models::prelude::*;
use prisma_query::ast::*;

pub struct MutationBuilder;

impl MutationBuilder {
    pub fn create_node(model: ModelRef, mut args: PrismaArgs) -> (Insert, Option<GraphqlId>) {
        let model_id = model.fields().id();

        let mut return_id: Option<GraphqlId> = None;

        if !model_id.is_auto_generated {
            let id = model.generate_id();
            args.insert(model_id.name.as_ref(), id.clone());
            return_id = Some(id)
        }

        let fields: Vec<&Field> = model
            .fields()
            .all
            .iter()
            .filter(|field| args.has_arg_for(&field.name()))
            .collect();

        let fields: Vec<(&str, PrismaValue)> = fields
            .iter()
            .map(|field| (field.name(), args.take_field_value(field.name()).unwrap()))
            .collect();

        let insert = fields
            .into_iter()
            .fold(Insert::into(model.table()), |query, (field_name, field_value)| {
                query.value(field_name, field_value)
            });

        (insert, return_id)
    }

    pub fn create_scalar_list_value(
        scalar_list_table: ScalarListTable,
        list_value: PrismaListValue,
        id: GraphqlId,
    ) -> Vec<Insert> {
        let positions = (1..=list_value.len()).map(|v| (v * 1000) as i64);
        let values = list_value.into_iter().zip(positions);

        values
            .map(|(value, position)| {
                Insert::into(scalar_list_table.table())
                    .value(ScalarListTable::POSITION_FIELD_NAME, position)
                    .value(ScalarListTable::VALUE_FIELD_NAME, value)
                    .value(ScalarListTable::NODE_ID_FIELD_NAME, id.clone())
            })
            .collect()
    }
}
