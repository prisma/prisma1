use super::Filter;
use crate::compare::ScalarCompare;
use prisma_models::{PrismaValue, ScalarField};
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct ScalarFilter {
    pub field: Arc<ScalarField>,
    pub condition: ScalarCondition,
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
    In(Option<Vec<PrismaValue>>),
    NotIn(Option<Vec<PrismaValue>>),
}

impl ScalarCompare for Arc<ScalarField> {
    /// Field is in a given value
    fn is_in<T>(&self, val: Option<Vec<T>>) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::In(val.map(|v| v.into_iter().map(|i| i.into()).collect())),
        })
    }

    /// Field is not in a given value
    fn not_in<T>(&self, val: Option<Vec<T>>) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::NotIn(val.map(|v| v.into_iter().map(|i| i.into()).collect())),
        })
    }

    /// Field equals the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.equals("foo");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::Equals(val) }) => {
    ///         assert_eq!(PrismaValue::from("foo"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn equals<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::Equals(val.into()),
        })
    }

    /// Field does not equal the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.not_equals(false);
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::NotEquals(val) }) => {
    ///         assert_eq!(PrismaValue::from(false), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn not_equals<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::NotEquals(val.into()),
        })
    }

    /// Field contains the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.contains("asdf");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::Contains(val) }) => {
    ///         assert_eq!(PrismaValue::from("asdf"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn contains<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::Contains(val.into()),
        })
    }

    /// Field does not contain the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.not_contains("asdf");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::NotContains(val) }) => {
    ///         assert_eq!(PrismaValue::from("asdf"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn not_contains<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::NotContains(val.into()),
        })
    }

    /// Field starts with the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.starts_with("qwert");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::StartsWith(val) }) => {
    ///         assert_eq!(PrismaValue::from("qwert"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn starts_with<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::StartsWith(val.into()),
        })
    }

    /// Field does not start with the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.not_starts_with("qwert");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::NotStartsWith(val) }) => {
    ///         assert_eq!(PrismaValue::from("qwert"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn not_starts_with<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::NotStartsWith(val.into()),
        })
    }

    /// Field ends with the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.ends_with("musti");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::EndsWith(val) }) => {
    ///         assert_eq!(PrismaValue::from("musti"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn ends_with<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::EndsWith(val.into()),
        })
    }

    /// Field does not end with the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("name").unwrap();
    /// let filter = field.not_ends_with("naukio");
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::NotEndsWith(val) }) => {
    ///         assert_eq!(PrismaValue::from("naukio"), val);
    ///         assert_eq!(String::from("name"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn not_ends_with<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::NotEndsWith(val.into()),
        })
    }

    /// Field is less than the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("id").unwrap();
    /// let filter = field.less_than(10);
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::LessThan(val) }) => {
    ///         assert_eq!(PrismaValue::from(10), val);
    ///         assert_eq!(String::from("id"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn less_than<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::LessThan(val.into()),
        })
    }

    /// Field is less than or equals the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("id").unwrap();
    /// let filter = field.less_than_or_equals(10);
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::LessThanOrEquals(val) }) => {
    ///         assert_eq!(PrismaValue::from(10), val);
    ///         assert_eq!(String::from("id"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn less_than_or_equals<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::LessThanOrEquals(val.into()),
        })
    }

    /// Field is greater than the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("id").unwrap();
    /// let filter = field.greater_than(10);
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::GreaterThan(val) }) => {
    ///         assert_eq!(PrismaValue::from(10), val);
    ///         assert_eq!(String::from("id"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn greater_than<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::GreaterThan(val.into()),
        })
    }

    /// Field is greater than or equals the given value.
    /// ```rust
    /// # use connector::{*, filter::*};
    /// # use prisma_models::*;
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("../sqlite-connector/test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let model = schema.find_model("User").unwrap();
    /// #
    /// let field = model.fields().find_from_scalar("id").unwrap();
    /// let filter = field.greater_than_or_equals(10);
    ///
    /// match filter {
    ///     Filter::Scalar(ScalarFilter { field: field, condition: ScalarCondition::GreaterThanOrEquals(val) }) => {
    ///         assert_eq!(PrismaValue::from(10), val);
    ///         assert_eq!(String::from("id"), field.name);
    ///     }
    ///     _ => unreachable!()
    /// }
    /// ```
    fn greater_than_or_equals<T>(&self, val: T) -> Filter
    where
        T: Into<PrismaValue>,
    {
        Filter::from(ScalarFilter {
            field: Arc::clone(self),
            condition: ScalarCondition::GreaterThanOrEquals(val.into()),
        })
    }
}
