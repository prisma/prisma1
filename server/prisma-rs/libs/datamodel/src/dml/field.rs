use super::comment::*;
use super::id::*;
use super::relation::*;
use super::scalar::*;
use super::traits::*;
use serde::{Serialize, Deserialize};

// This is duplicate for now, but explicitely required
// since we want to seperate ast and dml.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

// TODO: Maybe we include a seperate struct for relations which can be generic?
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum FieldType {
    Enum(String),
    Relation(RelationInfo),
    ConnectorSpecific {
        base_type: ScalarType,
        connector_type: Option<String>,
    },
    Base(ScalarType),
}

#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct IdInfo {
    pub strategy: IdStrategy,
    pub sequence: Option<Sequence>,
}

#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Field {
    pub name: String,
    pub arity: FieldArity,
    pub field_type: FieldType,
    pub database_name: Option<String>,
    pub default_value: Option<Value>,
    pub is_unique: bool,
    pub id_info: Option<IdInfo>,
    pub scalar_list_strategy: Option<ScalarListStrategy>,
    pub comments: Vec<Comment>
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
            comments: vec![]
        }
    }
}
