use super::field::*;
use super::traits::*;
use serde::{Deserialize, Serialize};

/// Represents a model in a prisma datamodel.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Model {
    /// Name of the model.
    pub name: String,
    /// Fields of the model.
    pub fields: Vec<Field>,
    /// Comments associated with this model.
    pub documentation: Option<String>,
    /// The database internal name of this model.
    pub database_name: Option<String>,
    /// Indicates if this model is embedded or not.
    pub is_embedded: bool,
    /// Indicates if this model is generated.
    pub is_generated: bool,
}

impl Model {
    /// Creates a new model with the given name.
    pub fn new(name: &str) -> Model {
        Model {
            name: String::from(name),
            fields: vec![],
            documentation: None,
            database_name: None,
            is_embedded: false,
            is_generated: false,
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

    /// Finds the name of all id fields
    pub fn id_field_names(&self) -> impl std::iter::Iterator<Item = &String> {
        self.fields().filter(|x| x.id_info.is_some()).map(|x| &x.name)
    }

    /// Finds the name of all id fields
    pub fn id_fields(&self) -> impl std::iter::Iterator<Item = &Field> {
        self.fields().filter(|x| x.id_info.is_some())
    }

    /// Finds a field with a certain relation guarantee.
    /// exclude_field are necessary to avoid corner cases with self-relations (e.g. we must not recognize a field as its own related field).
    pub fn related_field(&self, to: &str, name: &str, exclude_field: &str) -> Option<&Field> {
        self.fields().find(|f| {
            if let FieldType::Relation(rel_info) = &f.field_type {
                if rel_info.to == to && rel_info.name == name && (self.name != to || f.name != exclude_field) {
                    return true;
                }
            }
            false
        })
    }

    /// Finds a mutable field with a certain relation guarantee.
    pub fn related_field_mut(&mut self, to: &str, name: &str, exclude_field: &str) -> Option<&mut Field> {
        let self_name = self.name.clone();
        self.fields_mut().find(|f| {
            if let FieldType::Relation(rel_info) = &f.field_type {
                if rel_info.to == to && rel_info.name == name && (self_name != to || f.name != exclude_field) {
                    return true;
                }
            }

            false
        })
    }

    /// Checks if this is a relation model. A relation model has exactly
    /// two relations, which are required.
    pub fn is_relation_model(&self) -> bool {
        let related_fields = self.fields().filter(|f| -> bool {
            if let FieldType::Relation(_) = f.field_type {
                f.arity == FieldArity::Required
            } else {
                false
            }
        });

        related_fields.count() == 2
    }

    /// Checks if this is a pure relation model.
    /// It has only two fields, both of them are required relations.
    pub fn is_pure_relation_model(&self) -> bool {
        self.is_relation_model() && self.fields.len() == 2
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
