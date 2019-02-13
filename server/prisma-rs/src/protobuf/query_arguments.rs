use sql::{
    grammar::{
        clause::ConditionTree,
        database_value::{DatabaseValue, ToDatabaseValue},
    },
    prelude::*,
};

use crate::protobuf::prisma::{
    filter::Type, graphql_id::IdValue, scalar_filter::Condition, value_container::PrismaValue,
    AndFilter, Filter, NotFilter, OrFilter, ScalarFilter,
};

impl Into<ConditionTree> for ScalarFilter {
    fn into(self) -> ConditionTree {
        let field: &str = self.field.as_ref();

        match self.condition.unwrap() {
            Condition::Equals(value) => {
                ConditionTree::single(field.equals(value.prisma_value.unwrap()))
            }
            Condition::NotEquals(value) => {
                ConditionTree::single(field.not_equals(value.prisma_value.unwrap()))
            }
            Condition::Contains(value) => {
                ConditionTree::single(field.like(value.prisma_value.unwrap()))
            }
            Condition::NotContains(value) => {
                ConditionTree::single(field.not_like(value.prisma_value.unwrap()))
            }
            Condition::StartsWith(value) => {
                ConditionTree::single(field.begins_with(value.prisma_value.unwrap()))
            }
            Condition::NotStartsWith(value) => {
                ConditionTree::single(field.not_begins_with(value.prisma_value.unwrap()))
            }
            Condition::EndsWith(value) => {
                ConditionTree::single(field.ends_into(value.prisma_value.unwrap()))
            }
            Condition::NotEndsWith(value) => {
                ConditionTree::single(field.not_ends_into(value.prisma_value.unwrap()))
            }
            Condition::LessThan(value) => {
                ConditionTree::single(field.less_than(value.prisma_value.unwrap()))
            }
            Condition::LessThanOrEquals(value) => {
                ConditionTree::single(field.less_than_or_equals(value.prisma_value.unwrap()))
            }
            Condition::GreaterThan(value) => {
                ConditionTree::single(field.greater_than(value.prisma_value.unwrap()))
            }
            Condition::GreaterThanOrEquals(value) => {
                ConditionTree::single(field.greater_than_or_equals(value.prisma_value.unwrap()))
            }
            Condition::In(mc) => ConditionTree::single(
                field.in_selection(
                    mc.values
                        .into_iter()
                        .map(|v| v.prisma_value.unwrap())
                        .collect(),
                ),
            ),
            Condition::NotIn(mc) => ConditionTree::single(
                field.not_in_selection(
                    mc.values
                        .into_iter()
                        .map(|v| v.prisma_value.unwrap())
                        .collect(),
                ),
            ),
        }
    }
}

impl Into<ConditionTree> for AndFilter {
    fn into(mut self) -> ConditionTree {
        match self.filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right: ConditionTree = filter.into();

                self.filters.into_iter().rev().fold(right, |acc, filter| {
                    let left: ConditionTree = filter.into();
                    ConditionTree::and(left, acc)
                })
            }
        }
    }
}

impl Into<ConditionTree> for OrFilter {
    fn into(mut self) -> ConditionTree {
        match self.filters.pop() {
            None => ConditionTree::NoCondition,
            Some(filter) => {
                let right: ConditionTree = filter.into();

                self.filters.into_iter().rev().fold(right, |acc, filter| {
                    let left: ConditionTree = filter.into();
                    ConditionTree::or(left, acc)
                })
            }
        }
    }
}

impl Into<ConditionTree> for NotFilter {
    fn into(self) -> ConditionTree {
        let cond: ConditionTree = AndFilter {
            filters: self.filters,
        }
        .into();

        ConditionTree::not(cond)
    }
}

impl Into<ConditionTree> for Filter {
    fn into(self) -> ConditionTree {
        match self.type_.unwrap() {
            Type::And(and_filter) => and_filter.into(),
            Type::Or(or_filter) => or_filter.into(),
            Type::Not(not_filter) => not_filter.into(),
            Type::Scalar(scalar_filter) => scalar_filter.into(),
            Type::BoolFilter(b) => {
                if b {
                    ConditionTree::NoCondition
                } else {
                    ConditionTree::NegativeCondition
                }
            }
            _ => panic!("And, Or and Scalar are supported at this point"),
        }
    }
}

impl ToDatabaseValue for PrismaValue {
    fn to_database_value(self) -> DatabaseValue {
        match self {
            PrismaValue::String(s) => s.to_database_value(),
            PrismaValue::Float(f) => (f as f64).to_database_value(),
            PrismaValue::Boolean(b) => b.to_database_value(),
            PrismaValue::DateTime(d) => d.to_database_value(),
            PrismaValue::Enum(e) => e.to_database_value(),
            PrismaValue::Json(j) => j.to_database_value(),
            PrismaValue::Int(i) => (i as i64).to_database_value(),
            PrismaValue::Relation(i) => i.to_database_value(),
            PrismaValue::Null(_) => DatabaseValue::Null,
            PrismaValue::Uuid(u) => u.to_database_value(),
            PrismaValue::GraphqlId(id) => match id.id_value.unwrap() {
                IdValue::String(s) => s.to_database_value(),
                IdValue::Int(i) => i.to_database_value(),
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use sql::grammar::{clause::ConditionTree, Operation};

    use crate::protobuf::prisma::{
        filter::Type, scalar_filter::Condition, value_container::PrismaValue, AndFilter, Filter,
        MultiContainer, NotFilter, OrFilter, ScalarFilter, ValueContainer,
    };

    impl Filter {
        fn bool_filter(condition: bool) -> Filter {
            Filter {
                type_: Some(Type::BoolFilter(condition)),
            }
        }

        fn scalar(field: &str, condition: Condition) -> Filter {
            Filter {
                type_: Some(Type::Scalar(ScalarFilter {
                    field: field.to_string(),
                    condition: Some(condition),
                })),
            }
        }

        fn equals(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::Equals(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn not_equals(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::NotEquals(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn less_than(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::LessThan(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn less_than_or_equals(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::LessThanOrEquals(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn greater_than(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::GreaterThan(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn greater_than_or_equals(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::GreaterThanOrEquals(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn in_selection(field: &str, selection: Vec<PrismaValue>) -> Filter {
            Self::scalar(
                field,
                Condition::In(MultiContainer {
                    values: selection
                        .into_iter()
                        .map(|pv| ValueContainer {
                            prisma_value: Some(pv),
                        })
                        .collect(),
                }),
            )
        }

        fn not_in_selection(field: &str, selection: Vec<PrismaValue>) -> Filter {
            Self::scalar(
                field,
                Condition::NotIn(MultiContainer {
                    values: selection
                        .into_iter()
                        .map(|pv| ValueContainer {
                            prisma_value: Some(pv),
                        })
                        .collect(),
                }),
            )
        }

        fn contains(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::Contains(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn not_contains(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::NotContains(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn starts_with(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::StartsWith(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn not_starts_with(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::NotStartsWith(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn ends_with(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::EndsWith(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn not_ends_with(field: &str, equals: PrismaValue) -> Filter {
            Self::scalar(
                field,
                Condition::NotEndsWith(ValueContainer {
                    prisma_value: Some(equals),
                }),
            )
        }

        fn and(filters: Vec<Filter>) -> Filter {
            Filter {
                type_: Some(Type::And(AndFilter { filters })),
            }
        }

        fn or(filters: Vec<Filter>) -> Filter {
            Filter {
                type_: Some(Type::Or(OrFilter { filters })),
            }
        }

        fn not(filters: Vec<Filter>) -> Filter {
            Filter {
                type_: Some(Type::Not(NotFilter { filters })),
            }
        }
    }

    #[test]
    fn test_true() {
        let condition: ConditionTree = Filter::bool_filter(true).into();

        let sql = condition.compile().unwrap();

        assert_eq!("1=1", sql);
    }

    #[test]
    fn test_false() {
        let condition: ConditionTree = Filter::bool_filter(false).into();

        let sql = condition.compile().unwrap();

        assert_eq!("1=0", sql);
    }

    #[test]
    fn test_equals() {
        let condition: ConditionTree = Filter::equals("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo = 1", sql);
    }

    #[test]
    fn test_not_equals() {
        let condition: ConditionTree = Filter::not_equals("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo <> 1", sql);
    }

    #[test]
    fn test_less_than() {
        let condition: ConditionTree = Filter::less_than("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo < 1", sql);
    }

    #[test]
    fn test_less_than_or_equals() {
        let condition: ConditionTree =
            Filter::less_than_or_equals("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo <= 1", sql);
    }

    #[test]
    fn test_greater_than() {
        let condition: ConditionTree = Filter::greater_than("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo > 1", sql);
    }

    #[test]
    fn test_greater_than_or_equals() {
        let condition: ConditionTree =
            Filter::greater_than_or_equals("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo >= 1", sql);
    }

    #[test]
    fn test_contains() {
        let condition: ConditionTree =
            Filter::contains("foo", PrismaValue::String("bar".to_string())).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo LIKE '%bar%'", sql);
    }

    #[test]
    fn test_not_contains() {
        let condition: ConditionTree =
            Filter::not_contains("foo", PrismaValue::String("bar".to_string())).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo NOT LIKE '%bar%'", sql);
    }

    #[test]
    fn test_in() {
        let condition: ConditionTree =
            Filter::in_selection("foo", vec![PrismaValue::Int(1), PrismaValue::Int(2)]).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo IN (1, 2)", sql);
    }

    #[test]
    fn test_not_in() {
        let condition: ConditionTree = Filter::not_in_selection(
            "foo",
            vec![
                PrismaValue::String(String::from("foo")),
                PrismaValue::String(String::from("bar")),
            ],
        )
        .into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo NOT IN ('foo', 'bar')", sql);
    }

    #[test]
    fn test_starts_with() {
        let condition: ConditionTree =
            Filter::starts_with("foo", PrismaValue::String("bar".to_string())).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo LIKE 'bar%'", sql);
    }

    #[test]
    fn test_not_starts_with() {
        let condition: ConditionTree =
            Filter::not_starts_with("foo", PrismaValue::String("bar".to_string())).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo NOT LIKE 'bar%'", sql);
    }

    #[test]
    fn test_ends_with() {
        let condition: ConditionTree =
            Filter::ends_with("foo", PrismaValue::String("bar".to_string())).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo LIKE '%bar'", sql);
    }

    #[test]
    fn test_not_ends_with() {
        let condition: ConditionTree =
            Filter::not_ends_with("foo", PrismaValue::String("bar".to_string())).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo NOT LIKE '%bar'", sql);
    }

    #[test]
    fn test_empty_and() {
        let filter = Filter::and(Vec::new());
        let condition: ConditionTree = filter.into();

        assert_eq!("1=1", condition.compile().unwrap());
    }

    #[test]
    fn test_and_with_one_filter() {
        let filter = Filter::and(vec![Filter::equals("foo", PrismaValue::Boolean(false))]);

        let condition: ConditionTree = filter.into();

        assert_eq!("foo = false", condition.compile().unwrap());
    }

    #[test]
    fn test_and_with_two_filters() {
        let filter = Filter::and(vec![
            Filter::equals("foo", PrismaValue::Boolean(false)),
            Filter::equals("bar", PrismaValue::Int(2)),
        ]);

        let condition: ConditionTree = filter.into();

        assert_eq!("(foo = false AND bar = 2)", condition.compile().unwrap());
    }

    #[test]
    fn test_not_two_filters() {
        let filter = Filter::not(vec![
            Filter::equals("foo", PrismaValue::Boolean(false)),
            Filter::equals("bar", PrismaValue::Int(2)),
        ]);

        let condition: ConditionTree = filter.into();

        assert_eq!(
            "(NOT (foo = false AND bar = 2))",
            condition.compile().unwrap()
        );
    }

    #[test]
    fn test_and_with_three_filters() {
        let filter = Filter::and(vec![
            Filter::equals("foo", PrismaValue::Boolean(false)),
            Filter::equals("bar", PrismaValue::Int(2)),
            Filter::equals("lol", PrismaValue::String(String::from("wtf"))),
        ]);

        let condition: ConditionTree = filter.into();

        assert_eq!(
            "(foo = false AND (bar = 2 AND lol = 'wtf'))",
            condition.compile().unwrap()
        );
    }

    #[test]
    fn test_nested_and_or() {
        let and_1 = Filter::and(vec![
            Filter::equals("foo", PrismaValue::Boolean(false)),
            Filter::equals("bar", PrismaValue::Int(2)),
        ]);

        let and_2 = Filter::and(vec![
            Filter::equals("musti", PrismaValue::String(String::from("cat"))),
            Filter::equals("naukio", PrismaValue::String(String::from("cat"))),
        ]);

        let filter = Filter::or(vec![and_1, and_2]);
        let condition: ConditionTree = filter.into();

        assert_eq!(
            "((foo = false AND bar = 2) OR (musti = 'cat' AND naukio = 'cat'))",
            condition.compile().unwrap(),
        );
    }
}
