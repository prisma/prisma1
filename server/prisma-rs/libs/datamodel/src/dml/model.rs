use super::comment::*;
use super::field::*;
use super::traits::*;
use serde::{Deserialize, Serialize};

/// Represents a model in a prisma datamodel.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Model {
    /// Name of the model.
    pub name: String,
    /// Fields of the model.
    fields: Vec<Field>,
    /// Comments associated with this model.
    pub comments: Vec<Comment>,
    /// The database internal name of this model.
    pub database_name: Option<String>,
    /// Indicates if this model is embedded or not.
    pub is_embedded: bool,
}

impl Model {
    /// Creates a new model with the given name.
    pub fn new(name: &str) -> Model {
        Model {
            name: String::from(name),
            fields: vec![],
            comments: vec![],
            database_name: None,
            is_embedded: false,
        }
    }

    /// Adds a field to this model.
    pub fn add_field(&mut self, field: Field) {
        self.fields.push(field)
    }

    /// Removes a field with the given name from this model.
    pub fn remove_field(&mut self, name: &str) {
        self.fields.retain(|f| f.name != name);
    }

    /// Gets an iterator over all fields.
    pub fn fields(&self) -> std::slice::Iter<Field> {
        self.fields.iter()
    }

    /// Gets a mutable iterator over all fields.
    pub fn fields_mut(&mut self) -> std::slice::IterMut<Field> {
        self.fields.iter_mut()
    }

    /// Finds a field by name.
    pub fn find_field(&self, name: &str) -> Option<&Field> {
        self.fields().find(|f| f.name == *name)
    }

    /// Finds a field by name and returns a mutable reference.
    pub fn find_field_mut(&mut self, name: &str) -> Option<&mut Field> {
        self.fields_mut().find(|f| f.name == *name)
    }
}

impl WithName for Model {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &str) {
        self.name = String::from(name)
    }
}

impl WithDatabaseName for Model {
    fn database_name(&self) -> &Option<String> {
        &self.database_name
    }
    fn set_database_name(&mut self, database_name: &Option<String>) {
        self.database_name = database_name.clone()
    }
}
