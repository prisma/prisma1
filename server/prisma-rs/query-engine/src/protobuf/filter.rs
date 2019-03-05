use super::prisma as pb;
use prisma_models::prelude::*;

pub trait IntoFilter {
    fn into_filter(self, model: ModelRef) -> Filter;
}

impl IntoFilter for pb::Filter {
    fn into_filter(self, model: ModelRef) -> Filter {
        match self.type_.unwrap() {
            pb::filter::Type::And(and) => Filter::And(
                and.filters
                    .into_iter()
                    .map(|filter| Box::new(filter.into_filter(model.clone())))
                    .collect(),
            ),
            pb::filter::Type::Or(or) => Filter::Or(
                or.filters
                    .into_iter()
                    .map(|filter| Box::new(filter.into_filter(model.clone())))
                    .collect(),
            ),
            pb::filter::Type::Not(not) => Filter::Not(
                not.filters
                    .into_iter()
                    .map(|filter| Box::new(filter.into_filter(model.clone())))
                    .collect(),
            ),
            pb::filter::Type::Scalar(scalar) => scalar.into_filter(model),
            pb::filter::Type::ScalarList(scalar_list) => scalar_list.into_filter(model),
            pb::filter::Type::OneRelationIsNull(relation_field) => {
                let field = model
                    .fields()
                    .find_from_relation_fields(relation_field.field.as_ref())
                    .unwrap();

                Filter::OneRelationIsNull(OneRelationIsNullFilter { field })
            }
            pb::filter::Type::Relation(relation_filter) => relation_filter.into_filter(model),
            pb::filter::Type::NodeSubscription(_) => Filter::NodeSubscription,
            pb::filter::Type::BoolFilter(boo) => Filter::BoolFilter(boo),
        }
    }
}

impl IntoFilter for pb::ScalarFilter {
    fn into_filter(self, model: ModelRef) -> Filter {
        use pb::scalar_filter::Condition::*;

        let field = dbg!(model.fields()).find_from_scalar(self.field.as_ref()).unwrap();

        let condition = match self.condition.unwrap() {
            Equals(value) => ScalarCondition::Equals(value.into()),
            NotEquals(value) => ScalarCondition::NotEquals(value.into()),
            Contains(value) => ScalarCondition::Contains(value.into()),
            NotContains(value) => ScalarCondition::NotContains(value.into()),
            StartsWith(value) => ScalarCondition::StartsWith(value.into()),
            NotStartsWith(value) => ScalarCondition::NotStartsWith(value.into()),
            EndsWith(value) => ScalarCondition::EndsWith(value.into()),
            NotEndsWith(value) => ScalarCondition::NotEndsWith(value.into()),
            LessThan(value) => ScalarCondition::LessThan(value.into()),
            LessThanOrEquals(value) => ScalarCondition::LessThanOrEquals(value.into()),
            GreaterThan(value) => ScalarCondition::GreaterThan(value.into()),
            GreaterThanOrEquals(value) => ScalarCondition::GreaterThanOrEquals(value.into()),
            In(mc) => ScalarCondition::In(mc.values.into_iter().map(|value| value.into()).collect()),
            NotIn(mc) => ScalarCondition::NotIn(mc.values.into_iter().map(|value| value.into()).collect()),
        };

        Filter::Scalar(ScalarFilter { field, condition })
    }
}

impl IntoFilter for pb::ScalarListFilter {
    fn into_filter(self, model: ModelRef) -> Filter {
        use pb::scalar_list_condition::Condition::*;

        let field = model.fields().find_from_scalar(self.field.as_ref()).unwrap();

        let condition = match self.condition.condition.unwrap() {
            Contains(value) => ScalarListCondition::Contains(value.into()),
            ContainsEvery(values) => {
                ScalarListCondition::ContainsEvery(values.values.into_iter().map(|value| value.into()).collect())
            }
            ContainsSome(values) => {
                ScalarListCondition::ContainsSome(values.values.into_iter().map(|value| value.into()).collect())
            }
        };

        Filter::ScalarList(ScalarListFilter { field, condition })
    }
}

impl IntoFilter for pb::RelationFilter {
    fn into_filter(self, model: ModelRef) -> Filter {
        let condition = self.condition().clone();

        let field = model
            .fields()
            .find_from_relation_fields(self.field.field.as_ref())
            .unwrap();

        let nested_filter: Box<Filter> = Box::new((*self.nested_filter).into_filter(field.related_model()));

        let condition = match condition {
            pb::relation_filter::Condition::EveryRelatedNode => RelationCondition::EveryRelatedNode,
            pb::relation_filter::Condition::AtLeastOneRelatedNode => RelationCondition::AtLeastOneRelatedNode,
            pb::relation_filter::Condition::NoRelatedNode => RelationCondition::NoRelatedNode,
            pb::relation_filter::Condition::ToOneRelatedNode => RelationCondition::ToOneRelatedNode,
        };

        Filter::Relation(RelationFilter {
            field,
            nested_filter,
            condition,
        })
    }
}
