use super::attachment::*;
use super::comment::*;
use super::traits::*;

#[derive(Debug, PartialEq, Clone)]
pub struct Enum<Types: TypePack> {
    pub name: String,
    pub values: Vec<String>,
    pub comments: Vec<Comment>,
    pub attachment: Types::EnumAttachment,
}

impl<Types: TypePack> Enum<Types> {
    pub fn new(name: String, values: Vec<String>) -> Enum<Types> {
        Enum {
            name: name,
            values: values,
            comments: vec![],
            attachment: Types::EnumAttachment::default(),
        }
    }
}

impl<Types: TypePack> WithName for Enum<Types> {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &String) {
        self.name = name.clone()
    }
}
