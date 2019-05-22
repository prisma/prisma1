use super::comment::*;
use super::traits::*;
use serde::{Deserialize, Serialize};

/// Represents an enum in the datamodel.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Enum {
    /// Name of the enum.
    pub name: String,
    /// Values of the enum.
    pub values: Vec<String>,
    /// Comments for this enum.
    pub comments: Vec<Comment>,
    /// Database internal name of this enum.
    pub database_name: Option<String>,
}

impl Enum {
    /// Creates a new enum with the given name and values.
    pub fn new(name: &str, values: Vec<String>) -> Enum {
        Enum {
            name: String::from(name),
            values: values,
            comments: vec![],
            database_name: None,
        }
    }
}

impl WithName for Enum {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &str) {
        self.name = String::from(name)
    }
}

impl WithDatabaseName for Enum {
    fn database_name(&self) -> &Option<String> {
        &self.database_name
    }
    fn set_database_name(&mut self, database_name: &Option<String>) {
        self.database_name = database_name.clone()
    }
}
