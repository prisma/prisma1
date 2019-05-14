use super::comment::*;
use super::field::*;
use super::traits::*;
use serde::{Serialize, Deserialize};

#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Model {
    pub name: String,
    fields: Vec<Field>,
    pub comments: Vec<Comment>,
    pub database_name: Option<String>,
    pub is_embedded: bool
}

impl Model {
    pub fn new(name: &str) -> Model {
        Model {
            name: String::from(name),
            fields: vec![],
            comments: vec![],
            database_name: None,
            is_embedded: false
        }
    }

    pub fn add_field(&mut self, field: Field) {
        self.fields.push(field)
    }

    pub fn fields(&self) -> std::slice::Iter<Field> {
        self.fields.iter()
    }

    pub fn fields_mut(&mut self) -> std::slice::IterMut<Field> {
        self.fields.iter_mut()
    }

    pub fn find_field(&self, name: &str) -> Option<&Field> {
        self.fields().find(|f| f.name == *name)
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
