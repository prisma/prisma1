mod envelope;
mod input;
mod interface;
mod query_arguments;

pub mod prelude;

pub use envelope::ProtoBufEnvelope;
pub use input::*;
pub use interface::{ExternalInterface, ProtoBufInterface};
use prisma_models::ModelRef;
use prisma_query::ast::*;

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

use crate::{Error as CrateError, PrismaValue};
use prelude::*;

impl RpcResponse {
    pub fn header() -> Header {
        Header {
            type_name: String::from("RpcResponse"),
        }
    }

    pub fn empty() -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(prisma::Result { value: None })),
        }
    }

    pub fn ok(result: NodesResult) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(prisma::Result {
                value: Some(result::Value::NodesResult(result)),
            })),
        }
    }

    pub fn error(error: CrateError) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Error(ProtoError {
                value: Some(error.into()),
            })),
        }
    }
}

impl SelectedField {
    pub fn is_scalar(&self) -> bool {
        match self.field {
            Some(selected_field::Field::Scalar(_)) => true,
            _ => false,
        }
    }

    pub fn is_relational(&self) -> bool {
        match self.field {
            Some(selected_field::Field::Relational { .. }) => true,
            _ => false,
        }
    }
}

impl Node {
    pub fn get(&self, index: usize) -> Option<&PrismaValue> {
        self.values
            .get(index)
            .and_then(|ref vc| vc.prisma_value.as_ref())
    }

    pub fn len(&self) -> usize {
        self.values.len()
    }
}

impl ValueContainer {
    pub fn is_null_value(&self) -> bool {
        match self.prisma_value {
            Some(PrismaValue::Null(_)) => true,
            _ => false,
        }
    }
}

impl RelationFilter {
    pub fn as_sub_select(self, model: ModelRef) -> ConditionTree {
        let condition = self.condition();
        let column_name = model.db_name().to_string();
        let sub_select = self.relation_filter_sub_select(model);

        Self::in_statement_for_relation_condition(Column::from(column_name), condition, sub_select)
    }

    fn in_statement_for_relation_condition(
        column: Column,
        condition: relation_filter::Condition,
        sub_select: Select,
    ) -> ConditionTree {
        match condition {
            relation_filter::Condition::EveryRelatedNode => column.not_in_selection(sub_select),
            relation_filter::Condition::NoRelatedNode => column.not_in_selection(sub_select),
            relation_filter::Condition::AtLeastOneRelatedNode => column.in_selection(sub_select),
            relation_filter::Condition::ToOneRelatedNode => column.in_selection(sub_select),
        }
        .into()
    }

    fn relation_filter_sub_select(self, model: ModelRef) -> Select {
        let relation_field = model
            .fields()
            .find_from_relation(&self.field.field)
            .expect("Relation not found");

        let relation = relation_field.relation();
        let invert_condition_of_subselect = match self.condition() {
            relation_filter::Condition::EveryRelatedNode => true,
            _ => false,
        };

        let this_relation_column = relation.column_for_relation_side(relation_field.relation_side);
        let other_relation_column =
            relation.column_for_relation_side(relation_field.related_field().relation_side);
        let relation_table_name = relation.relation_table_name();
        let schema = model.schema();

        match self.nested_filter.type_ {
            Some(filter::Type::Relation(nested)) => {
                let condition = nested.condition();
                let nested_select = nested.relation_filter_sub_select(model.clone());

                let condition_tree = Self::in_statement_for_relation_condition(
                    Column::from((
                        schema.db_name.as_ref(),
                        relation_table_name.as_ref(),
                        other_relation_column.as_ref(),
                    )),
                    condition,
                    nested_select,
                );

                Select::from((schema.db_name.as_ref(), relation_table_name.as_ref()))
                    .column(Column::from((
                        schema.db_name.as_ref(),
                        relation_table_name.as_ref(),
                        this_relation_column.as_ref(),
                    )))
                    .so_that(condition_tree.invert_if(invert_condition_of_subselect))
            }
            _ => {
                let filter: ConditionTree =
                    (*self.nested_filter).into_condition_tree(model.clone());

                let join_conditions = relation_field.related_model().id_column().equals(
                    relation.column_for_relation_side(relation_field.relation_side.opposite()),
                );

                let join = model.table().on(join_conditions);

                Select::from((schema.db_name.as_ref(), relation_table_name.as_ref()))
                    .column(this_relation_column)
                    .inner_join(join)
                    .so_that(filter.invert_if(invert_condition_of_subselect))
            }
        }
    }
}
