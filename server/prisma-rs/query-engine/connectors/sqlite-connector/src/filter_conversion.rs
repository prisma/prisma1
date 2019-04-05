use connector::{Filter, OneRelationIsNullFilter, RelationCondition, RelationFilter, ScalarCondition, ScalarFilter};
use prisma_models::prelude::*;
use prisma_query::ast::*;

#[derive(Clone, Copy, Debug)]
pub enum AliasMode {
    Table,
    Join,
}

impl Default for AliasMode {
    fn default() -> Self {
        AliasMode::Table
    }
}

#[derive(Clone, Copy, Debug, Default)]
pub struct Alias {
    counter: usize,
    mode: AliasMode,
}

impl Alias {
    fn inc(&self, mode: AliasMode) -> Self {
        Self {
            counter: self.counter + 1,
            mode,
        }
    }

    fn dec(&self) -> Self {
        let counter = if self.counter == 0 { 0 } else { self.counter - 1 };

        Self {
            counter,
            mode: self.mode,
        }
    }

    fn flip(&self, mode: AliasMode) -> Self {
        Self {
            counter: self.counter,
            mode,
        }
    }

    fn to_string(&self, mode_override: Option<AliasMode>) -> String {
        match mode_override.unwrap_or(self.mode) {
            AliasMode::Table => format!("t{}", self.counter),
            AliasMode::Join => format!("j{}", self.counter),
        }
    }
}

pub trait AliasedCondition {
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree;
}

trait AliasedSelect {
    fn aliased_sel(self, alias: Option<Alias>) -> Select;
}

impl AliasedCondition for Filter {
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree {
        match self {
            Filter::And(mut filters) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right = (*filter).aliased_cond(alias);

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left = (*filter).aliased_cond(alias);
                        ConditionTree::and(left, acc)
                    })
                }
            },
            Filter::Or(mut filters) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right = (*filter).aliased_cond(alias);

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left = (*filter).aliased_cond(alias);
                        ConditionTree::or(left, acc)
                    })
                }
            },
            Filter::Not(mut filters) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right = (*filter).aliased_cond(alias).not();

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left = (*filter).aliased_cond(alias).not();
                        ConditionTree::and(left, acc)
                    })
                }
            },
            Filter::Scalar(filter) => filter.aliased_cond(alias),
            Filter::OneRelationIsNull(filter) => filter.aliased_cond(alias),
            Filter::Relation(filter) => filter.aliased_cond(alias),
            Filter::BoolFilter(b) => {
                if b {
                    ConditionTree::NoCondition
                } else {
                    ConditionTree::NegativeCondition
                }
            }
            _ => unimplemented!(),
        }
    }
}

impl AliasedCondition for ScalarFilter {
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree {
        let column = match alias {
            Some(ref alias) => self.field.as_column().table(alias.to_string(None)),
            None => self.field.as_column(),
        };

        let condition = match self.condition {
            ScalarCondition::Equals(PrismaValue::Null) => column.is_null(),
            ScalarCondition::NotEquals(PrismaValue::Null) => column.is_not_null(),
            ScalarCondition::Equals(value) => column.equals(value),
            ScalarCondition::NotEquals(value) => column.not_equals(value),
            ScalarCondition::Contains(value) => column.like(format!("{}", value)),
            ScalarCondition::NotContains(value) => column.not_like(format!("{}", value)),
            ScalarCondition::StartsWith(value) => column.begins_with(format!("{}", value)),
            ScalarCondition::NotStartsWith(value) => column.not_begins_with(format!("{}", value)),
            ScalarCondition::EndsWith(value) => column.ends_into(format!("{}", value)),
            ScalarCondition::NotEndsWith(value) => column.not_ends_into(format!("{}", value)),
            ScalarCondition::LessThan(value) => column.less_than(value),
            ScalarCondition::LessThanOrEquals(value) => column.less_than_or_equals(value),
            ScalarCondition::GreaterThan(value) => column.greater_than(value),
            ScalarCondition::GreaterThanOrEquals(value) => column.greater_than_or_equals(value),
            ScalarCondition::In(values) => match values.split_first() {
                Some((PrismaValue::Null, tail)) if tail.is_empty() => column.is_null(),
                _ => column.in_selection(values),
            },
            ScalarCondition::NotIn(values) => match values.split_first() {
                Some((PrismaValue::Null, tail)) if tail.is_empty() => column.is_not_null(),
                _ => column.not_in_selection(values),
            },
        };

        ConditionTree::single(condition)
    }
}

impl AliasedCondition for RelationFilter {
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree {
        let id = self.field.model().id_column();

        let column = match alias {
            Some(ref alias) => id.table(alias.dec().to_string(None)),
            None => id,
        };

        let condition = self.condition.clone();
        let sub_select = self.aliased_sel(alias.map(|a| a.inc(AliasMode::Table)));

        let comparison = match condition {
            RelationCondition::EveryRelatedNode => column.not_in_selection(sub_select),
            RelationCondition::NoRelatedNode => column.not_in_selection(sub_select),
            RelationCondition::AtLeastOneRelatedNode => column.in_selection(sub_select),
            RelationCondition::ToOneRelatedNode => column.in_selection(sub_select),
        };

        comparison.into()
    }
}

impl AliasedSelect for RelationFilter {
    fn aliased_sel(self, alias: Option<Alias>) -> Select {
        let alias = alias.unwrap_or(Alias::default());
        let condition = self.condition.clone();
        let relation = self.field.relation();

        let this_column = self.field.relation_column().table(alias.to_string(None));
        let other_column = self.field.opposite_column().table(alias.to_string(None));

        match *self.nested_filter {
            Filter::Relation(filter) => {
                let sub_condition = filter.condition.clone();
                let sub_select = filter.aliased_sel(Some(alias.inc(AliasMode::Table)));

                let tree: ConditionTree = match sub_condition {
                    RelationCondition::EveryRelatedNode => other_column.not_in_selection(sub_select),
                    RelationCondition::NoRelatedNode => other_column.not_in_selection(sub_select),
                    RelationCondition::AtLeastOneRelatedNode => other_column.in_selection(sub_select),
                    RelationCondition::ToOneRelatedNode => other_column.in_selection(sub_select),
                }
                .into();

                let conditions = tree.invert_if(condition.invert_of_subselect());

                Select::from_table(relation.relation_table().alias(alias.to_string(None)))
                    .column(this_column)
                    .so_that(conditions)
            }
            nested_filter => {
                let tree = nested_filter.aliased_cond(Some(alias.flip(AliasMode::Join)));

                let id_column = self
                    .field
                    .related_model()
                    .id_column()
                    .table(alias.to_string(Some(AliasMode::Join)));

                let join = self
                    .field
                    .related_model()
                    .table()
                    .alias(alias.to_string(Some(AliasMode::Join)))
                    .on(id_column.equals(other_column));

                let table = relation.relation_table().alias(alias.to_string(Some(AliasMode::Table)));

                Select::from_table(table)
                    .column(this_column)
                    .inner_join(join)
                    .so_that(tree.invert_if(condition.invert_of_subselect()))
            }
        }
    }
}

impl AliasedCondition for OneRelationIsNullFilter {
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree {
        let alias = alias.map(|a| a.to_string(None));

        let condition = if self.field.relation_is_inlined_in_parent() {
            self.field.as_column().opt_table(alias.clone()).is_null()
        } else {
            let relation = self.field.relation();

            let column = relation
                .column_for_relation_side(self.field.relation_side)
                .opt_table(alias.clone());

            let table = Table::from(relation.relation_table());
            let relation_table = match alias {
                Some(ref alias) => table.alias(alias.to_string()),
                None => table,
            };

            let select = Select::from_table(relation_table).column(column);
            let id_column = self.field.model().id_column().opt_table(alias.clone());

            id_column.not_in_selection(select)
        };

        ConditionTree::single(condition)
    }
}
