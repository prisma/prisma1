use super::attachment::*;
use super::model::*;
use super::enummodel::*;
use super::comment::*;

// TODO: Is schema the right name here?
#[derive(Debug, PartialEq, Clone)]
pub struct Schema<Types: TypePack> {
    enums: Vec<Enum<Types>>,
    models: Vec<Model<Types>>,
    pub comments: Vec<Comment>,
    pub attachment: Types::SchemaAttachment
}

impl<Types: TypePack> Schema<Types> {
    pub fn new() -> Schema<Types> {
        Schema {
            models: vec![],
            enums: vec![],
            comments: vec![],
            attachment: Types::SchemaAttachment::default()
        }
    }

    pub fn empty() -> Schema<Types> {
        Self::new()
    }

    pub fn has_model(&self, name: &String) -> bool {
        match self.find_model(name) {
            Some(_) => true,
            None => false
        }
    }

    pub fn add_enum(&mut self, en: Enum<Types>) {
        self.enums.push(en);
    }

    pub fn add_model(&mut self, model: Model<Types>) {
        self.models.push(model);
    }

    pub fn models(&self) -> std::slice::Iter<Model<Types>> {
        self.models.iter()
    }

    pub fn enums(&self) -> std::slice::Iter<Enum<Types>> {
        self.enums.iter()
    }

    pub fn models_mut(&mut self) -> std::slice::IterMut<Model<Types>> {
        self.models.iter_mut()
    }

    pub fn enums_mut(&mut self) -> std::slice::IterMut<Enum<Types>> {
        self.enums.iter_mut()
    }

    pub fn find_model(&self, name: &String) -> Option<&Model<Types>> {
        self.models().find(|m| m.name == *name)
    }

    pub fn find_enum(&self, name: &String) -> Option<&Enum<Types>> {
        self.enums().find(|m| m.name == *name)
    }
}
