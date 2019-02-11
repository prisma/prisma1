use sql::{
    grammar::{
        clause::ConditionTree,
        database_value::{DatabaseValue, ToDatabaseValue},
    },
    prelude::*,
};

use crate::protobuf::prisma::{
    filter::Type, graphql_id::IdValue, scalar_filter::Condition, value_container::PrismaValue,
    AndFilter, Filter, NotFilter, OrFilter, RelationFilter,
};

impl Type {
    // SLFWOOFC
    fn strip_logical_filters_with_only_one_filter_contained(self) -> Type {
        let strip_filter = move |filter: Filter| {
            let type_ = filter
                .type_
                .unwrap()
                .strip_logical_filters_with_only_one_filter_contained();
            Filter { type_: Some(type_) }
        };

        let strip_filters =
            move |filters: Vec<Filter>| filters.into_iter().map(strip_filter).collect();

        match self {
            Type::And(AndFilter { mut filters }) => {
                if filters.len() == 1 {
                    let filter = filters.pop().unwrap();
                    filter
                        .type_
                        .unwrap()
                        .strip_logical_filters_with_only_one_filter_contained()
                } else {
                    Type::And(AndFilter {
                        filters: strip_filters(filters),
                    })
                }
            }
            Type::Or(OrFilter { mut filters }) => {
                if filters.len() == 1 {
                    let filter = filters.pop().unwrap();
                    filter
                        .type_
                        .unwrap()
                        .strip_logical_filters_with_only_one_filter_contained()
                } else {
                    Type::Or(OrFilter {
                        filters: strip_filters(filters),
                    })
                }
            }
            Type::Not(NotFilter { filters }) => Type::Not(NotFilter {
                filters: strip_filters(filters),
            }),
            Type::Relation(rf) => match *rf {
                RelationFilter {
                    field,
                    nested_filter,
                    condition,
                } => {
                    let nested_filter = strip_filter(*nested_filter);

                    Type::Relation(Box::new(RelationFilter {
                        field,
                        nested_filter: Box::new(nested_filter),
                        condition,
                    }))
                }
            },
            x => x,
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

impl Into<ConditionTree> for Filter {
    fn into(self) -> ConditionTree {
        match self
            .type_
            .unwrap()
            .strip_logical_filters_with_only_one_filter_contained()
        {
            Type::And(AndFilter { mut filters }) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right: ConditionTree = filter.into();

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left: ConditionTree = filter.into();
                        ConditionTree::and(left, acc)
                    })
                }
            },
            Type::Or(OrFilter { mut filters }) => match filters.pop() {
                None => ConditionTree::NoCondition,
                Some(filter) => {
                    let right: ConditionTree = filter.into();

                    filters.into_iter().rev().fold(right, |acc, filter| {
                        let left: ConditionTree = filter.into();
                        ConditionTree::or(left, acc)
                    })
                }
            },
            Type::Scalar(scalar_filter) => match scalar_filter.condition.unwrap() {
                Condition::Equals(value) => {
                    let field: &str = scalar_filter.field.as_ref();

                    ConditionTree::single(field.equals(value.prisma_value.unwrap()))
                }
                _ => panic!("Only Equals is supported at this point"),
            },
            _ => panic!("And, Or and Scalar are supported at this point"),
        }
    }
}

#[cfg(test)]
mod tests {
    use sql::grammar::{clause::ConditionTree, Operation};

    use crate::protobuf::prisma::{
        filter::Type, scalar_filter::Condition, value_container::PrismaValue, AndFilter, Filter,
        OrFilter, ScalarFilter, ValueContainer,
    };

    impl Filter {
        fn new_scalar_equals(field: &str, equals: PrismaValue) -> Filter {
            Filter {
                type_: Some(Type::Scalar(ScalarFilter {
                    field: field.to_string(),
                    condition: Some(Condition::Equals(ValueContainer {
                        prisma_value: Some(equals),
                    })),
                })),
            }
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
    }

    #[test]
    fn test_simple_equals() {
        let condition: ConditionTree = Filter::new_scalar_equals("foo", PrismaValue::Int(1)).into();

        let sql = condition.compile().unwrap();

        assert_eq!("foo = 1", sql);
    }

    #[test]
    fn test_empty_and() {
        let filter = Filter::and(Vec::new());
        let condition: ConditionTree = filter.into();

        assert_eq!("true", condition.compile().unwrap());
    }

    #[test]
    fn test_and_with_one_filter() {
        let filter = Filter::and(vec![Filter::new_scalar_equals(
            "foo",
            PrismaValue::Boolean(false),
        )]);

        let condition: ConditionTree = filter.into();

        assert_eq!("foo = false", condition.compile().unwrap());
    }

    #[test]
    fn test_and_with_two_filters() {
        let filter = Filter::and(vec![
            Filter::new_scalar_equals("foo", PrismaValue::Boolean(false)),
            Filter::new_scalar_equals("bar", PrismaValue::Int(2)),
        ]);

        let condition: ConditionTree = filter.into();

        assert_eq!("(foo = false AND bar = 2)", condition.compile().unwrap());
    }

    #[test]
    fn test_and_with_three_filters() {
        let filter = Filter::and(vec![
            Filter::new_scalar_equals("foo", PrismaValue::Boolean(false)),
            Filter::new_scalar_equals("bar", PrismaValue::Int(2)),
            Filter::new_scalar_equals("lol", PrismaValue::String(String::from("wtf"))),
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
            Filter::new_scalar_equals("foo", PrismaValue::Boolean(false)),
            Filter::new_scalar_equals("bar", PrismaValue::Int(2)),
        ]);

        let and_2 = Filter::and(vec![
            Filter::new_scalar_equals("musti", PrismaValue::String(String::from("cat"))),
            Filter::new_scalar_equals("naukio", PrismaValue::String(String::from("cat"))),
        ]);

        let filter = Filter::or(vec![and_1, and_2]);
        let condition: ConditionTree = filter.into();

        assert_eq!(
            "((foo = false AND bar = 2) OR (musti = 'cat' AND naukio = 'cat'))",
            condition.compile().unwrap(),
        );
    }
}
