use super::comment::*;
use super::id::*;
use super::relation::*;
use super::scalar::*;
use super::traits::*;
use serde::{Deserialize, Serialize};

/// Datamodel field arity.
#[derive(Debug, PartialEq, Copy, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

/// Datamodel field type.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub enum FieldType {
    /// This is an enum field, with an enum of the given name.
    Enum(String),
    /// This is a relation field.
    Relation(RelationInfo),
    /// Connector specific field type.
    ConnectorSpecific {
        base_type: ScalarType,
        connector_type: Option<String>,
    },
    /// Base (built-in scalar) type.
    Base(ScalarType),
}

/// Holds information about an id, or priamry key.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct IdInfo {
    /// The strategy which is used to generate the id field.
    pub strategy: IdStrategy,
    /// A sequence used to generate the id.
    pub sequence: Option<Sequence>,
}

/// Represents a field in a model.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Field {
    /// Name of the field.
    pub name: String,
    /// The field's arity.
    pub arity: FieldArity,
    /// The field's type.
    pub field_type: FieldType,
    /// The database internal name.
    pub database_name: Option<String>,
    /// The default value.
    pub default_value: Option<Value>,
    /// Indicates if the field is unique.
    pub is_unique: bool,
    /// If set, signals that this field is an id field, or
    /// primary key.
    pub id_info: Option<IdInfo>,
    /// Strategy for representing scalar lists. Only valid if
    /// the field arity is list and the type is scalar.
    pub scalar_list_strategy: Option<ScalarListStrategy>,
    /// Comments associated with this field.
    pub comments: Vec<Comment>,
    /// If set, signals that this field was internally generated
    /// and should never be displayed to the user.
    pub is_generated: bool,
}

impl WithName for Field {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &str) {
        self.name = String::from(name)
    }
}

impl WithDatabaseName for Field {
    fn database_name(&self) -> &Option<String> {
        &self.database_name
    }
    fn set_database_name(&mut self, database_name: &Option<String>) {
        self.database_name = database_name.clone()
    }
}

impl Field {
    /// Creates a new field with the given name and type.
    pub fn new(name: &str, field_type: FieldType) -> Field {
        Field {
            name: String::from(name),
            arity: FieldArity::Required,
            field_type: field_type,
            database_name: None,
            default_value: None,
            is_unique: false,
            id_info: None,
            scalar_list_strategy: None,
            comments: vec![],
            is_generated: false,
        }
    }
    /// Creates a new field with the given name and type, marked as generated and optional.
    pub fn new_generated(name: &str, field_type: FieldType) -> Field {
        Field {
            name: String::from(name),
            arity: FieldArity::Optional,
            field_type: field_type,
            database_name: None,
            default_value: None,
            is_unique: false,
            id_info: None,
            scalar_list_strategy: None,
            comments: vec![],
            is_generated: true,
        }
    }
}
