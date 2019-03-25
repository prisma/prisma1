use prisma_models::{PrismaValue, RelationField, ScalarField};
use std::sync::Arc;

#[derive(Debug, Clone)]
pub enum Filter {
    And(Vec<Box<Filter>>),
    Or(Vec<Box<Filter>>),
    Not(Vec<Box<Filter>>),
    Scalar(ScalarFilter),
    ScalarList(ScalarListFilter),
    OneRelationIsNull(OneRelationIsNullFilter),
    Relation(RelationFilter),
    NodeSubscription,
    BoolFilter(bool),
}

#[derive(Debug, Clone)]
pub enum ScalarCondition {
    Equals(PrismaValue),
    NotEquals(PrismaValue),
    Contains(PrismaValue),
    NotContains(PrismaValue),
    StartsWith(PrismaValue),
    NotStartsWith(PrismaValue),
    EndsWith(PrismaValue),
    NotEndsWith(PrismaValue),
    LessThan(PrismaValue),
    LessThanOrEquals(PrismaValue),
    GreaterThan(PrismaValue),
    GreaterThanOrEquals(PrismaValue),
    In(Vec<PrismaValue>),
    NotIn(Vec<PrismaValue>),
}

#[derive(Debug, Clone)]
pub enum ScalarListCondition {
    Contains(PrismaValue),
    ContainsEvery(Vec<PrismaValue>),
    ContainsSome(Vec<PrismaValue>),
}

#[derive(Debug, Clone)]
pub struct OneRelationIsNullFilter {
    pub field: Arc<RelationField>,
}
#[derive(Debug, Clone)]
pub struct ScalarListFilter {
    pub field: Arc<ScalarField>,
    pub condition: ScalarListCondition,
}

#[derive(Debug, Clone)]
pub struct ScalarFilter {
    pub field: Arc<ScalarField>,
    pub condition: ScalarCondition,
}
#[derive(Debug, Clone)]
pub struct RelationFilter {
    pub field: Arc<RelationField>,
    pub nested_filter: Box<Filter>,
    pub condition: RelationCondition,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub enum RelationCondition {
    EveryRelatedNode,
    AtLeastOneRelatedNode,
    NoRelatedNode,
    ToOneRelatedNode,
}

impl RelationCondition {
    pub fn invert_of_subselect(&self) -> bool {
        match self {
            RelationCondition::EveryRelatedNode => true,
            _ => false,
        }
    }
}
