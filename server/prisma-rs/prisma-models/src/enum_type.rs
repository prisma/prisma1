use super::{InternalEnum, OrderBy, ScalarField, SortOrder};
use serde::{
    de::{self, Visitor},
    Deserialize, Deserializer, Serialize, Serializer,
};
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
#[derive(Debug, Clone, PartialEq)]
pub struct EnumValue {
    pub name: String,
    pub value: EnumValueWrapper,
}

impl EnumValue {
    /// Represents this enum value as string.
    pub fn as_string(&self) -> String {
        match &self.value {
            EnumValueWrapper::String(s) => s.clone(),
            EnumValueWrapper::OrderBy(ob) => format!("{}_{}", ob.field.name, ob.sort_order.abbreviated()),
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

impl From<String> for EnumValue {
    fn from(s: String) -> Self {
        EnumValue::string(s.clone(), s)
    }
}

impl Serialize for EnumValue {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(self.as_string().as_str())
    }
}

impl<'de> Deserialize<'de> for EnumValue {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_any(EnumValueVisitor)
    }
}

/// Custom deserialization
struct EnumValueVisitor;

impl<'de> Visitor<'de> for EnumValueVisitor {
    type Value = EnumValue;

    fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
        formatter.write_str("A string.")
    }

    fn visit_str<E>(self, value: &str) -> Result<EnumValue, E>
    where
        E: de::Error,
    {
        Ok(EnumValue::string(value.to_owned(), value.to_owned()))
    }
}

#[derive(Debug, Clone)]
pub enum EnumValueWrapper {
    OrderBy(OrderBy),
    String(String),
}

impl PartialEq for EnumValueWrapper {
    fn eq(&self, _other: &Self) -> bool {
        false // WIP
    }
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
