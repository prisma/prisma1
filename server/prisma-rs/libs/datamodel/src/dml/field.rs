use super::attachment::*;
use super::comment::*;
use super::scalar::*;
use super::relation::*;
use super::id::*;
use super::traits::*;

// This is duplicate for now, but explicitely required
// since we want to seperate ast and dml.
#[derive(Debug, PartialEq, Clone)]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

// TODO: Maybe we include a seperate struct for relations which can be generic?
#[derive(Debug, Clone, PartialEq)]
pub enum FieldType<Types: TypePack> {
    Enum { enum_type: String },
    Relation(RelationInfo<Types>),
    ConnectorSpecific { base_type: ScalarType, connector_type: Option<String> },
    Base(ScalarType)
}

#[derive(Debug, PartialEq, Clone)]
pub struct IdInfo {
    pub strategy: Option<IdStrategy>,
    pub sequence: Option<Sequence>,
}

#[derive(Debug, PartialEq, Clone)]
pub struct Field<Types: TypePack> {
    pub name: String,
    pub arity: FieldArity,
    pub field_type: FieldType<Types>,
    pub database_name: Option<String>,
    pub default_value: Option<Value>,
    pub is_unique: bool,
    pub id_info: Option<IdInfo>,
    pub scalar_list_strategy: Option<ScalarListStrategy>,
    pub comments: Vec<Comment>,
    pub attachment: Types::FieldAttachment
}

impl<Types: TypePack> WithName for Field<Types> {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &String) {
        self.name = name.clone()
    }
}

impl<Types: TypePack> WithDatabaseName for Field<Types> {
    fn database_name(&self) -> &Option<String> {
        &self.database_name
    }
    fn set_database_name(&mut self, database_name: &Option<String>) {
        self.database_name = database_name.clone()
    }
}

impl<Types: TypePack> Field<Types> {
    pub fn new(name: String, field_type: FieldType<Types>) -> Field<Types> {
        Field {
            name: name,
            arity: FieldArity::Required,
            field_type: field_type,
            database_name: None,
            default_value: None,
            is_unique: false,
            id_info: None,
            scalar_list_strategy: None,
            comments: vec![],
            attachment: Types::FieldAttachment::default(),
        }
    }
}
