use crate::NodeSelector;
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

impl Filter {
    pub fn and(filters: Vec<Filter>) -> Self {
        Filter::And(filters.into_iter().map(Box::new).collect())
    }

    pub fn or(filters: Vec<Filter>) -> Self {
        Filter::Or(filters.into_iter().map(Box::new).collect())
    }

    pub fn not(filters: Vec<Filter>) -> Self {
        Filter::Not(filters.into_iter().map(Box::new).collect())
    }
}

impl From<ScalarFilter> for Filter {
    fn from(sf: ScalarFilter) -> Self {
        Filter::Scalar(sf)
    }
}

impl From<ScalarListFilter> for Filter {
    fn from(sf: ScalarListFilter) -> Self {
        Filter::ScalarList(sf)
    }
}

impl From<OneRelationIsNullFilter> for Filter {
    fn from(sf: OneRelationIsNullFilter) -> Self {
        Filter::OneRelationIsNull(sf)
    }
}

impl From<RelationFilter> for Filter {
    fn from(sf: RelationFilter) -> Self {
        Filter::Relation(sf)
    }
}

impl From<bool> for Filter {
    fn from(b: bool) -> Self {
        Filter::BoolFilter(b)
    }
}

impl From<NodeSelector> for Filter {
    fn from(node_selector: NodeSelector) -> Self {
        Filter::Scalar(ScalarFilter {
            field: node_selector.field,
            condition: ScalarCondition::Equals(node_selector.value),
        })
    }
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
