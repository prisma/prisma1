use super::comment::*;
use super::enummodel::*;
use super::model::*;
use serde::{Deserialize, Serialize};

// TODO: Is schema the right name here?
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Schema {
    enums: Vec<Enum>,
    models: Vec<Model>,
    pub comments: Vec<Comment>,
}

impl Schema {
    pub fn new() -> Schema {
        Schema {
            models: vec![],
            enums: vec![],
            comments: vec![],
        }
    }

    pub fn empty() -> Schema {
        Self::new()
    }

    pub fn has_model(&self, name: &str) -> bool {
        match self.find_model(name) {
            Some(_) => true,
            None => false,
        }
    }

    pub fn add_enum(&mut self, en: Enum) {
        self.enums.push(en);
    }

    pub fn remove_enum(&mut self, name: &str) {
        self.enums.retain(|m| m.name != name);
    }

    pub fn add_model(&mut self, model: Model) {
        self.models.push(model);
    }

    pub fn remove_model(&mut self, name: &str) {
        self.models.retain(|m| m.name != name);
    }

    pub fn models(&self) -> std::slice::Iter<Model> {
        self.models.iter()
    }

    pub fn enums(&self) -> std::slice::Iter<Enum> {
        self.enums.iter()
    }

    pub fn models_mut(&mut self) -> std::slice::IterMut<Model> {
        self.models.iter_mut()
    }

    pub fn enums_mut(&mut self) -> std::slice::IterMut<Enum> {
        self.enums.iter_mut()
    }

    pub fn find_model(&self, name: &str) -> Option<&Model> {
        self.models().find(|m| m.name == *name)
    }

    pub fn find_enum(&self, name: &str) -> Option<&Enum> {
        self.enums().find(|m| m.name == *name)
    }

    pub fn find_model_mut(&mut self, name: &str) -> Option<&mut Model> {
        self.models_mut().find(|m| m.name == *name)
    }

    pub fn find_enum_mut(&mut self, name: &str) -> Option<&mut Enum> {
        self.enums_mut().find(|m| m.name == *name)
    }
}
