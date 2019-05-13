use super::attachment::*;
use super::model::*;
use super::enummodel::*;
use super::comment::*;
use super::traits::*;
// TODO: Is schema the right name here?

#[derive(Debug, PartialEq, Clone)]
pub enum ModelOrEnum<Types: TypePack> {
    Enum(Enum<Types>),
    Model(Model<Types>)
}

#[derive(Debug, PartialEq, Clone)]
pub struct Schema<Types: TypePack> {
    pub models: Vec<ModelOrEnum<Types>>,
    pub comments: Vec<Comment>,
    pub attachment: Types::SchemaAttachment
}

impl<Types: TypePack> Schema<Types> {
    pub fn new() -> Schema<Types> {
        Schema {
            models: vec![],
            comments: vec![],
            attachment: Types::SchemaAttachment::default()
        }
    }

    pub fn empty() -> Schema<Types> {
        Self::new()
    }

    pub fn has_model(&self, name: String) -> bool {
        for model in &self.models {
            match model {
                ModelOrEnum::Model(m) => {
                    if(m.name() == &name) {
                        return true;
                    }
                },
                _ => {},
            }
        }
        false
    }

    pub fn models(&self) -> Vec<Model<Types>> {
        let mut result = Vec::new();
        for model in &self.models {
            match model {
                ModelOrEnum::Model(m) => result.push(m.clone()),
                _ => {},
            }
        }
        result
    }

    pub fn find_model(&self, name: String) -> Option<Model<Types>> {
        self.models().iter().find(|m| m.name == name).map(|m| m.clone())
    }
}
