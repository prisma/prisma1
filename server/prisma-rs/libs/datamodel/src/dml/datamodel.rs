use super::comment::*;
use super::enummodel::*;
use super::field::Field;
use super::model::*;
use serde::{Deserialize, Serialize};

// TODO: Is schema the right name here?
/// Represents a prisma-datamodel.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Datamodel {
    // All enums.
    pub enums: Vec<Enum>,
    // All models.
    pub models: Vec<Model>,
    /// All comments.
    pub comments: Vec<Comment>,
}

/// Type alias for (ModelName, FieldName)
pub type FieldRef = (String, String);

impl Datamodel {
    /// Creates a new, empty schema.
    pub fn new() -> Datamodel {
        Datamodel {
            models: vec![],
            enums: vec![],
            comments: vec![],
        }
    }

    /// Creates a new, empty schema.
    pub fn empty() -> Datamodel {
        Self::new()
    }

    /// Checks if a model with the given name exists.
    pub fn has_model(&self, name: &str) -> bool {
        match self.find_model(name) {
            Some(_) => true,
            None => false,
        }
    }

    /// Checks if an enum with the given name exists.
    pub fn has_enum(&self, name: &str) -> bool {
        match self.find_enum(name) {
            Some(_) => true,
            None => false,
        }
    }

    /// Adds an enum to this datamodel.
    pub fn add_enum(&mut self, en: Enum) {
        self.enums.push(en);
    }

    /// Removes an enum from this datamodel.
    pub fn remove_enum(&mut self, name: &str) {
        self.enums.retain(|m| m.name != name);
    }

    /// Adds a model to this datamodel.
    pub fn add_model(&mut self, model: Model) {
        self.models.push(model);
    }

    /// Removes a model from this datamodel.
    pub fn remove_model(&mut self, name: &str) {
        self.models.retain(|m| m.name != name);
    }

    /// Gets an iterator over all models.
    pub fn models(&self) -> std::slice::Iter<Model> {
        self.models.iter()
    }

    /// Gets an iterator over all enums.
    pub fn enums(&self) -> std::slice::Iter<Enum> {
        self.enums.iter()
    }

    /// Gets a mutable iterator over all models.
    pub fn models_mut(&mut self) -> std::slice::IterMut<Model> {
        self.models.iter_mut()
    }

    /// Gets a mutable iterator over all enums.
    pub fn enums_mut(&mut self) -> std::slice::IterMut<Enum> {
        self.enums.iter_mut()
    }

    /// Finds a model by name.
    pub fn find_model(&self, name: &str) -> Option<&Model> {
        self.models().find(|m| m.name == *name)
    }

    /// Finds a model for a field reference by using reference comparison.
    pub fn find_model_by_field_ref(&self, field: &Field) -> Option<&Model> {
        // This uses the memory location of field for equality.
        self.models()
            .find(|m| m.fields().any(|f| f as *const Field == field as *const Field))
    }

    /// Finds a field reference by a model and field name.
    pub fn find_field(&self, field: &FieldRef) -> Option<&Field> {
        // This uses the memory location of field for equality.
        self.find_model(&field.0)?.find_field(&field.1)
    }

    /// Finds a mutable field reference by a model and field name.
    pub fn find_field_mut(&mut self, field: &FieldRef) -> Option<&mut Field> {
        // This uses the memory location of field for equality.
        self.find_model_mut(&field.0)?.find_field_mut(&field.1)
    }

    /// Finds an enum by name.
    pub fn find_enum(&self, name: &str) -> Option<&Enum> {
        self.enums().find(|m| m.name == *name)
    }

    /// Finds a model by name and returns a mutable reference.
    pub fn find_model_mut(&mut self, name: &str) -> Option<&mut Model> {
        self.models_mut().find(|m| m.name == *name)
    }

    /// Finds an enum by name and returns a mutable reference.
    pub fn find_enum_mut(&mut self, name: &str) -> Option<&mut Enum> {
        self.enums_mut().find(|m| m.name == *name)
    }
}
