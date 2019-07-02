use connector::filter::*;
use prisma_models::prelude::*;
use prisma_query::ast::*;

#[derive(Clone, Copy, Debug)]
/// A distinction in aliasing to separate the parent table and the joined data
/// in the statement.
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
/// Aliasing tool to count the nesting level to help with heavily nested
/// self-related queries.
pub struct Alias {
    counter: usize,
    mode: AliasMode,
}

impl Alias {
    /// Increment the alias as a new copy.
    ///
    /// Use when nesting one level down to a new subquery. `AliasMode` is
    /// required due to the fact the current mode can be in `AliasMode::Join`.
    pub fn inc(&self, mode: AliasMode) -> Self {
        Self {
            counter: self.counter + 1,
            mode,
        }
    }

    /// Decrement the alias as a new copy keeping the same `AliasMode`.
    pub fn dec(&self) -> Self {
        let counter = if self.counter == 0 { 0 } else { self.counter - 1 };

        Self {
            counter,
            mode: self.mode,
        }
    }

    /// Flip the alias to a different mode keeping the same nesting count.
    pub fn flip(&self, mode: AliasMode) -> Self {
        Self {
            counter: self.counter,
            mode,
        }
    }

    /// A string representation of the current alias. The current mode can be
    /// overridden by defining the `mode_override`.
    pub fn to_string(&self, mode_override: Option<AliasMode>) -> String {
        match mode_override.unwrap_or(self.mode) {
            AliasMode::Table => format!("t{}", self.counter),
            AliasMode::Join => format!("j{}", self.counter),
        }
    }
}

pub trait AliasedCondition {
    /// Conversion to a query condition tree. Columns will point to the given
    /// alias if provided, otherwise using the fully qualified path.
    ///
    /// Alias should be used only when nesting, making the top level queries
    /// more explicit.
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree<'static>;
}

trait AliasedSelect {
    /// Conversion to a select. Columns will point to the given
    /// alias if provided, otherwise using the fully qualified path.
    ///
    /// Alias should be used only when nesting, making the top level queries
    /// more explicit.
    fn aliased_sel(self, alias: Option<Alias>) -> Select<'static>;
}

impl AliasedCondition for Filter {
    /// Conversion from a `Filter` to a query condition tree. Aliased when in a nested `SELECT`.
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree<'static> {
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
                None => ConditionTree::NegativeCondition,
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
    /// Conversion from a `ScalarFilter` to a query condition tree. Aliased when in a nested `SELECT`.
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree<'static> {
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
            // We need to preserve the split first semantic for protobuf
            ScalarCondition::In(Some(values)) => match values.split_first() {
                Some((PrismaValue::Null, tail)) if tail.is_empty() => column.is_null(),
                _ => column.in_selection(values),
            },
            // We need to preserve the split first semantic for protobuf
            ScalarCondition::NotIn(Some(values)) => match values.split_first() {
                Some((PrismaValue::Null, tail)) if tail.is_empty() => column.is_not_null(),
                _ => column.not_in_selection(values),
            },
            ScalarCondition::In(None) => column.is_null(),
            ScalarCondition::NotIn(None) => column.is_not_null(),
        };

        ConditionTree::single(condition)
    }
}

impl AliasedCondition for RelationFilter {
    /// Conversion from a `RelationFilter` to a query condition tree. Aliased when in a nested `SELECT`.
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree<'static> {
        let id = self.field.model().id_column();

        let column = match alias {
            Some(ref alias) => id.table(alias.dec().to_string(None)),
            None => id,
        };

        let condition = self.condition.clone();
        let sub_select = self.aliased_sel(alias.map(|a| a.inc(AliasMode::Table)));

        let comparison = match condition {
            RelationCondition::EveryRelatedRecord => column.not_in_selection(sub_select),
            RelationCondition::NoRelatedRecord => column.not_in_selection(sub_select),
            RelationCondition::AtLeastOneRelatedRecord => column.in_selection(sub_select),
            RelationCondition::ToOneRelatedRecord => column.in_selection(sub_select),
        };

        comparison.into()
    }
}

impl AliasedSelect for RelationFilter {
    /// The subselect part of the `RelationFilter` `ConditionTree`.
    fn aliased_sel(self, alias: Option<Alias>) -> Select<'static> {
        let alias = alias.unwrap_or(Alias::default());
        let condition = self.condition.clone();
        let relation = self.field.relation();

        let this_column = self.field.relation_column().table(alias.to_string(None));
        let other_column = self.field.opposite_column().table(alias.to_string(None));

        // Normalize filter tree
        let compacted = match *self.nested_filter {
            Filter::And(mut filters) => {
                if filters.len() == 1 {
                    *filters.pop().unwrap()
                } else {
                    Filter::And(filters)
                }
            }
            Filter::Or(mut filters) => {
                if filters.len() == 1 {
                    *filters.pop().unwrap()
                } else {
                    Filter::Or(filters)
                }
            }
            f => f,
        };

        match compacted {
            Filter::Relation(filter) => {
                let sub_condition = filter.condition.clone();
                let sub_select = filter.aliased_sel(Some(alias.inc(AliasMode::Table)));

                let tree: ConditionTree<'static> = match sub_condition {
                    RelationCondition::EveryRelatedRecord => other_column.not_in_selection(sub_select),
                    RelationCondition::NoRelatedRecord => other_column.not_in_selection(sub_select),
                    RelationCondition::AtLeastOneRelatedRecord => other_column.in_selection(sub_select),
                    RelationCondition::ToOneRelatedRecord => other_column.in_selection(sub_select),
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
    /// Conversion from a `OneRelationIsNullFilter` to a query condition tree. Aliased when in a nested `SELECT`.
    fn aliased_cond(self, alias: Option<Alias>) -> ConditionTree<'static> {
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
