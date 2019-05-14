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
    pub fn new(name: &str, values: Vec<String>) -> Enum<Types> {
        Enum {
            name: String::from(name),
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
    fn set_name(&mut self, name: &str) {
        self.name = String::from(name)
    }
}
