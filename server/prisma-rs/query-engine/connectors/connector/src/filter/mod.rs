//! Filtering types to select records from the database
//!
//! The creation of the types should be done with
//! [ScalarCompare](/connector/trait.ScalarCompare.html) and
//! [RelationCompare](/connector/trait.RelationCompare.html).

mod list;
mod node_selector;
mod relation;
mod scalar;

pub use list::*;
pub use node_selector::*;
pub use relation::*;
pub use scalar::*;

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
