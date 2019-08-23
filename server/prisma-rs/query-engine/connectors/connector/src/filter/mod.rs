//! Filtering types to select records from the database
//!
//! The creation of the types should be done with
//! [ScalarCompare](/connector/trait.ScalarCompare.html) and
//! [RelationCompare](/connector/trait.RelationCompare.html).

mod list;
mod record_finder;
mod relation;
mod scalar;

pub use list::*;
pub use record_finder::*;
pub use relation::*;
pub use scalar::*;

#[derive(Debug, Clone)]
pub enum Filter {
    And(Vec<Filter>),
    Or(Vec<Filter>),
    Not(Vec<Filter>),
    Scalar(ScalarFilter),
    ScalarList(ScalarListFilter),
    OneRelationIsNull(OneRelationIsNullFilter),
    Relation(RelationFilter),
    NodeSubscription,
    BoolFilter(bool),
}

impl Filter {
    pub fn and(filters: Vec<Filter>) -> Self {
        Filter::And(filters)
    }

    pub fn or(filters: Vec<Filter>) -> Self {
        Filter::Or(filters)
    }

    pub fn not(filters: Vec<Filter>) -> Self {
        Filter::Not(filters)
    }

    pub fn empty() -> Self {
        Filter::BoolFilter(true)
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

impl From<RecordFinder> for Filter {
    fn from(record_finder: RecordFinder) -> Self {
        Filter::Scalar(ScalarFilter {
            field: record_finder.field,
            condition: ScalarCondition::Equals(record_finder.value),
        })
    }
}
