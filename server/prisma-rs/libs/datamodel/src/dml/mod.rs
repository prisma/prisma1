// TODOs to answer together with rust teams:
// * Should this structure be mutatble or immutable?
// * Should this structure contain circular references? (Would make renaming models/fields MUCH easier)
// * How do we handle ocnnector specific settings, like indeces? Maybe inheritance, traits and having a Connector<T>?

use chrono::{DateTime, Utc};
use std::str::FromStr;
use validator::value::ValueParserError;

pub mod validator;

// TODO: Naming
pub trait Attachment : std::fmt::Debug + std::clone::Clone + std::cmp::PartialEq {
    fn default() -> Self;
}

#[derive(Debug, PartialEq, Clone)]
pub struct EmptyAttachment {}

impl Attachment for EmptyAttachment {
    fn default() -> Self { EmptyAttachment {} } 
}

// TODO: Better name
// TODO: Decide which attachments we really need.
pub trait TypePack : std::fmt::Debug + std::clone::Clone + std::cmp::PartialEq {
    type FieldAttachment : Attachment;
    type ModelAttachment : Attachment;
    type EnumAttachment : Attachment;
    type SchemaAttachment : Attachment;
    type RelationAttachment : Attachment;
}

#[derive(Debug, PartialEq, Clone)]
pub struct BuiltinTypePack { }

impl TypePack for BuiltinTypePack {
    type EnumAttachment = EmptyAttachment;
    type ModelAttachment = EmptyAttachment;
    type FieldAttachment = EmptyAttachment;
    type SchemaAttachment = EmptyAttachment;
    type RelationAttachment = EmptyAttachment;
}

#[derive(Debug, PartialEq, Clone)]
pub struct RelationInfo<Types: TypePack> { 
    pub to: String,
    pub to_field: String, 
    pub name: Option<String>, 
    pub on_delete: OnDeleteStrategy,
    pub attachment: Types::RelationAttachment
}


impl<Types: TypePack> RelationInfo<Types> {
    fn new(to: String, to_field: String) -> RelationInfo<Types> {
        RelationInfo {
            to: to,
            to_field: to_field,
            name: None,
            on_delete: OnDeleteStrategy::None,
            attachment: Types::RelationAttachment::default()
        }
    }
}



// Setters are a bit untypical for rust,
// but we want to have "composeable" struct creation.
pub trait WithName {
    fn name(&self) -> &String;
    fn set_name(&mut self, name: &String);
}

pub trait WithDatabaseName {
    fn database_name(&self) -> &Option<String>;
    fn set_database_name(&mut self, database_name: &Option<String>);
}

// This is duplicate for now, but explicitely required
// since we want to seperate ast and dml.
#[derive(Debug, PartialEq, Clone)]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

#[derive(Debug, PartialEq, Clone)]
pub struct Comment {
    pub text: String,
    pub is_error: bool,
}

#[derive(Debug, Copy, PartialEq, Clone)]
pub enum ScalarType {
    Int,
    Float,
    Decimal,
    Boolean,
    String,
    DateTime,
    Enum,
}

// TODO, Check if data types are correct
#[derive(Debug, PartialEq, Clone)]
pub enum Value {
    Int(i32),
    Float(f32),
    Decimal(f32),
    Boolean(bool),
    String(String),
    DateTime(DateTime<Utc>),
    ConstantLiteral(String),
}

// TODO: Maybe we include a seperate struct for relations which can be generic?
#[derive(Debug, Clone, PartialEq)]
pub enum FieldType<Types: TypePack> {
    Enum { enum_type: String },
    Relation(RelationInfo<Types>),
    ConnectorSpecific { base_type: ScalarType, connector_type: Option<String> },
    Base(ScalarType)
}

#[derive(Debug, Copy, PartialEq, Clone)]
pub enum IdStrategy {
    Auto,
    None,
}

impl FromStr for IdStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "AUTO" => Ok(IdStrategy::Auto),
            "NONE" => Ok(IdStrategy::None),
            _ => Err(ValueParserError::new(format!("Invalid id strategy {}.", s))),
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

impl FromStr for ScalarListStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "EMBEDDED" => Ok(ScalarListStrategy::Embedded),
            "RELATION" => Ok(ScalarListStrategy::Relation),
            _ => Err(ValueParserError::new(format!("Invalid scalar list strategy {}.", s))),
        }
    }
}

#[derive(Debug, Copy, PartialEq, Clone)]
pub enum OnDeleteStrategy {
    Cascade,
    None
}

impl FromStr for OnDeleteStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "CASCADE" => Ok(OnDeleteStrategy::Cascade),
            "NONE" => Ok(OnDeleteStrategy::None),
            _ => Err(ValueParserError::new(format!("Invalid onDelete strategy {}.", s)))
        }
    }
}


#[derive(Debug, PartialEq, Clone)]
pub struct Sequence {
    pub name: String,
    pub initial_value: i32,
    pub allocation_size: i32,
}

impl WithName for Sequence {
    fn name(&self) -> &String {
        &self.name
    }
    fn set_name(&mut self, name: &String) {
        self.name = name.clone()
    }
}

#[derive(Debug, PartialEq, Clone)]
pub struct Field<Types: TypePack> {
    pub name: String,
    pub arity: FieldArity,
    pub field_type: FieldType<Types>,
    pub database_name: Option<String>,
    pub default_value: Option<Value>,
    pub is_unique: bool,
    pub is_id: bool,
    pub id_strategy: Option<IdStrategy>,
    // TODO: Not sure if a sequence should be a member of field.
    pub id_sequence: Option<Sequence>,
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
    fn new(name: String, field_type: FieldType<Types>) -> Field<Types> {
        Field {
            name: name,
            arity: FieldArity::Required,
            field_type: field_type,
            database_name: None,
            default_value: None,
            is_unique: false,
            is_id: false,
            id_strategy: None,
            id_sequence: None,
            scalar_list_strategy: None,
            comments: vec![],
            attachment: Types::FieldAttachment::default(),
        }
    }
}

#[derive(Debug, PartialEq, Clone)]
pub struct Enum<Types: TypePack> {
    pub name: String,
    pub values: Vec<String>,
    pub comments: Vec<Comment>,
    pub attachment: Types::EnumAttachment
}

impl<Types: TypePack> Enum<Types> {
    fn new(name: String, values: Vec<String>) -> Enum<Types> {
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

#[derive(Debug, PartialEq, Clone)]
pub struct Model<Types: TypePack> {
    pub name: String,
    pub fields: Vec<Field<Types>>,
    pub comments: Vec<Comment>,
    pub database_name: Option<String>,
    pub is_embedded: bool,
    pub attachment: Types::ModelAttachment,
}

impl<Types: TypePack> Model<Types> {
    fn new(name: &String) -> Model<Types> {
        Model {
            name: name.clone(),
            fields: vec![],
            comments: vec![],
            database_name: None,
            is_embedded: false,
            attachment: Types::ModelAttachment::default()
        }
    }

    pub fn find_field(&self, name: String) -> Option<Field<Types>> {
        self.fields.iter().find(|f| f.name == name).map(|f| f.clone())
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
    fn new() -> Schema<Types> {
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
