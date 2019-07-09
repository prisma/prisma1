use super::{InternalEnum, OrderBy, ScalarField, SortOrder};
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct EnumType {
    pub name: String,
    pub values: Vec<EnumValue>,
}

impl EnumType {
    /// Attempts to find an enum value for the given value key.
    pub fn value_for(&self, name: &str) -> Option<&EnumValue> {
        self.values.iter().find(|val| val.name == name)
    }
}

/// Values in enums are solved with an enum rather than a trait or generic
/// to avoid cluttering all type defs in this file, essentially.
#[derive(Debug, Clone)]
pub struct EnumValue {
    pub name: String,
    pub value: EnumValueWrapper,
}

impl EnumValue {
    /// Attempts to represent this enum value as string.
    pub fn as_string(&self) -> Option<String> {
        match self.value {
            EnumValueWrapper::String(ref s) => Some(s.clone()),
            EnumValueWrapper::OrderBy(_) => None,
        }
    }

    pub fn order_by<T>(name: T, field: Arc<ScalarField>, sort_order: SortOrder) -> Self
    where
        T: Into<String>,
    {
        EnumValue {
            name: name.into(),
            value: EnumValueWrapper::OrderBy(OrderBy { field, sort_order }),
        }
    }

    pub fn string<T>(name: T, value: String) -> Self
    where
        T: Into<String>,
    {
        EnumValue {
            name: name.into(),
            value: EnumValueWrapper::String(value),
        }
    }
}

#[derive(Debug, Clone)]
pub enum EnumValueWrapper {
    OrderBy(OrderBy),
    String(String),
}

impl From<&InternalEnum> for EnumType {
    fn from(internal_enum: &InternalEnum) -> EnumType {
        let values = internal_enum
            .values
            .iter()
            .map(|v| EnumValue::string(v.clone(), v.clone()))
            .collect();

        EnumType {
            name: internal_enum.name.clone(),
            values,
        }
    }
}
