use super::attachment::*;
use super::field::*;
use super::comment::*;
use super::traits::*;

#[derive(Debug, PartialEq, Clone)]
pub struct Model<Types: TypePack> {
    pub name: String,
    fields: Vec<Field<Types>>,
    pub comments: Vec<Comment>,
    pub database_name: Option<String>,
    pub is_embedded: bool,
    pub attachment: Types::ModelAttachment,
}

impl<Types: TypePack> Model<Types> {
    pub fn new(name: &String) -> Model<Types> {
        Model {
            name: name.clone(),
            fields: vec![],
            comments: vec![],
            database_name: None,
            is_embedded: false,
            attachment: Types::ModelAttachment::default()
        }
    }

    pub fn add_field(&mut self, field: Field<Types>) {
        self.fields.push(field)
    }

    pub fn fields(&self) -> std::slice::Iter<Field<Types>> {
        self.fields.iter()
    }

    pub fn fields_mut(&mut self) -> std::slice::IterMut<Field<Types>> {
        self.fields.iter_mut()
    }

    pub fn find_field(&self, name: &String) -> Option<&Field<Types>> {
        self.fields().find(|f| f.name == *name)
    }
}

impl<Types: TypePack> WithName for Model<Types> {
    fn name(&self) -> &String { &self.name }
    fn set_name(&mut self, name: &String) { self.name = name.clone() }
}

impl<Types: TypePack> WithDatabaseName for Model<Types> {
    fn database_name(&self) -> &Option<String> { &self.database_name }
    fn set_database_name(&mut self, database_name: &Option<String>) { self.database_name = database_name.clone() }
}
