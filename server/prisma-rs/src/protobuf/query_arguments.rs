use prisma_query::ast::*;
use std::fmt;

use crate::protobuf::prelude::*;

impl Into<Order> for SortOrder {
    fn into(self) -> Order {
        match self {
            SortOrder::Asc => Order::Asc,
            SortOrder::Desc => Order::Desc,
        }
    }
}

impl fmt::Display for PrismaValue {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            PrismaValue::String(s) => s.fmt(f),
            PrismaValue::Float(s) => s.fmt(f),
            PrismaValue::Boolean(s) => s.fmt(f),
            PrismaValue::DateTime(s) => s.fmt(f),
            PrismaValue::Enum(s) => s.fmt(f),
            PrismaValue::Json(s) => s.fmt(f),
            PrismaValue::Int(s) => s.fmt(f),
            PrismaValue::Relation(s) => s.fmt(f),
            PrismaValue::Null(s) => s.fmt(f),
            PrismaValue::Uuid(s) => s.fmt(f),
            PrismaValue::GraphqlId(ref id) => match id.id_value.as_ref().unwrap() {
                IdValue::String(s) => s.fmt(f),
                IdValue::Int(s) => s.fmt(f),
            },
        }
    }
}

impl From<ScalarFilter> for ConditionTree {
    fn from(sf: ScalarFilter) -> ConditionTree {
        let field = sf.field;

        match sf.condition.unwrap() {
            scalar_filter::Condition::Equals(value) => match value.prisma_value.unwrap() {
                PrismaValue::Null(_) => ConditionTree::single(field.is_null()),
                val => ConditionTree::single(field.equals(val)),
            },
            scalar_filter::Condition::NotEquals(value) => match value.prisma_value.unwrap() {
                PrismaValue::Null(_) => ConditionTree::single(field.is_not_null()),
                val => ConditionTree::single(field.not_equals(val)),
            },
            scalar_filter::Condition::Contains(value) => {
                let val = format!("{}", value.prisma_value.unwrap());
                ConditionTree::single(field.like(val))
            }
            scalar_filter::Condition::NotContains(value) => {
                let val = format!("{}", value.prisma_value.unwrap());
                ConditionTree::single(field.not_like(val))
            }
            scalar_filter::Condition::StartsWith(value) => {
                let val = format!("{}", value.prisma_value.unwrap());
                ConditionTree::single(field.begins_with(val))
            }
            scalar_filter::Condition::NotStartsWith(value) => {
                let val = format!("{}", value.prisma_value.unwrap());
                ConditionTree::single(field.not_begins_with(val))
            }
            scalar_filter::Condition::EndsWith(value) => {
                let val = format!("{}", value.prisma_value.unwrap());
                ConditionTree::single(field.ends_into(val))
            }
            scalar_filter::Condition::NotEndsWith(value) => {
                let val = format!("{}", value.prisma_value.unwrap());
                ConditionTree::single(field.not_ends_into(val))
            }
            scalar_filter::Condition::LessThan(value) => {
                let val: DatabaseValue = value.prisma_value.unwrap().into();
                ConditionTree::single(field.less_than(val))
            }
            scalar_filter::Condition::LessThanOrEquals(value) => {
                let val: DatabaseValue = value.prisma_value.unwrap().into();
                ConditionTree::single(field.less_than_or_equals(val))
            }
            scalar_filter::Condition::GreaterThan(value) => {
                let val: DatabaseValue = value.prisma_value.unwrap().into();
                ConditionTree::single(field.greater_than(val))
            }
            scalar_filter::Condition::GreaterThanOrEquals(value) => {
                let val: DatabaseValue = value.prisma_value.unwrap().into();
                ConditionTree::single(field.greater_than_or_equals(val))
            }
            scalar_filter::Condition::In(mc) => match mc.values.split_first() {
                Some((head, tail)) if tail.is_empty() && head.is_null_value() => {
                    ConditionTree::single(field.is_null())
                }
                _ => ConditionTree::single(
                    field.in_selection(
                        mc.values
                            .into_iter()
                            .map(|v| v.prisma_value.unwrap())
                            .collect::<Vec<PrismaValue>>(),
                    ),
                ),
            },
            scalar_filter::Condition::NotIn(mc) => match mc.values.split_first() {
                Some((head, tail)) if tail.is_empty() && head.is_null_value() => {
                    ConditionTree::single(field.is_not_null())
                }
                _ => ConditionTree::single(
                    field.not_in_selection(
                        mc.values
                            .into_iter()
                            .map(|v| v.prisma_value.unwrap())
                            .collect::<Vec<PrismaValue>>(),
                    ),
                ),
            },
        }
    }
}

impl From<AndFilter> for ConditionTree {
    fn from(mut and: AndFilter) -> ConditionTree {
        match and.filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right: ConditionTree = filter.into();

                and.filters.into_iter().rev().fold(right, |acc, filter| {
                    let left: ConditionTree = filter.into();
                    ConditionTree::and(left, acc)
                })
            }
        }
    }
}

impl From<OrFilter> for ConditionTree {
    fn from(mut or: OrFilter) -> ConditionTree {
        match or.filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right: ConditionTree = filter.into();

                or.filters.into_iter().rev().fold(right, |acc, filter| {
                    let left: ConditionTree = filter.into();
                    ConditionTree::or(left, acc)
                })
            }
        }
    }
}

impl From<NotFilter> for ConditionTree {
    fn from(not: NotFilter) -> ConditionTree {
        let cond: ConditionTree = AndFilter {
            filters: not.filters,
        }
        .into();

        ConditionTree::not(cond)
    }
}

impl From<RelationFilter> for ConditionTree {
    fn from(_rf: RelationFilter) -> ConditionTree {
        unimplemented!()
    }
}

impl From<Filter> for ConditionTree {
    fn from(f: Filter) -> ConditionTree {
        match f.type_.unwrap() {
            filter::Type::And(and_filter) => and_filter.into(),
            filter::Type::Or(or_filter) => or_filter.into(),
            filter::Type::Not(not_filter) => not_filter.into(),
            filter::Type::Scalar(scalar_filter) => scalar_filter.into(),
            filter::Type::BoolFilter(b) => {
                if b {
                    ConditionTree::NoCondition
                } else {
                    ConditionTree::NegativeCondition
                }
            }
            filter::Type::Relation(relation_filter) => (*relation_filter).into(),
            e => panic!("Unsupported filter: {:?}", e),
        }
    }
}

impl Into<DatabaseValue> for IdValue {
    fn into(self) -> DatabaseValue {
        match self {
            graphql_id::IdValue::String(s) => s.into(),
            graphql_id::IdValue::Int(i) => i.into(),
        }
    }
}

impl Into<DatabaseValue> for PrismaValue {
    fn into(self) -> DatabaseValue {
        match self {
            PrismaValue::String(s) => s.into(),
            PrismaValue::Float(f) => (f as f64).into(),
            PrismaValue::Boolean(b) => b.into(),
            PrismaValue::DateTime(d) => d.into(),
            PrismaValue::Enum(e) => e.into(),
            PrismaValue::Json(j) => j.into(),
            PrismaValue::Int(i) => (i as i64).into(),
            PrismaValue::Relation(i) => i.into(),
            PrismaValue::Null(_) => DatabaseValue::Parameterized(ParameterizedValue::Null),
            PrismaValue::Uuid(u) => u.into(),
            PrismaValue::GraphqlId(id) => id.id_value.unwrap().into(),
        }
    }
}
