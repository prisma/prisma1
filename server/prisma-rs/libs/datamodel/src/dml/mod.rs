// TODOs to answer together with rust teams:
// * Should this structure be mutatble or immutable?
// * Should this structure contain circular references? (Would make renaming models/fields MUCH easier)
// * How do we handle ocnnector specific settings, like indeces? Maybe inheritance, traits and having a Connector<T>?

use chrono::{DateTime, Utc};
use std::str::FromStr;
use validator::value::ValueParserError;

pub mod validator;

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
#[derive(Debug)]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

#[derive(Debug)]
pub struct Comment {
    pub text: String,
    pub is_error: bool
}

#[derive(Debug, Copy, Clone)]
pub enum ScalarType {
    Int,
    Float, 
    Decimal,
    Boolean,
    String,
    DateTime,
    Enum
}

// TODO, Check if data types are correct
#[derive(Debug)]
pub enum Value {
    Int(i32),
    Float(f32),
    Decimal(f32),
    Boolean(bool),
    String(String),
    DateTime(DateTime<Utc>),
    ConstantLiteral(String)
}

#[derive(Debug, Clone)]
pub enum FieldType {
    Enum { enum_type: String },
    Relation { to: String, to_field: String, name: Option<String> },
    ConnectorSpecific { base_type: ScalarType, connector_type: Option<String> },
    Base(ScalarType)
}

#[derive(Debug, Copy, Clone)]
pub enum IdStrategy {
    Auto,
    None
}

impl FromStr for IdStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "AUTO" => Ok(IdStrategy::Auto),
            "NONE" => Ok(IdStrategy::None),
            _ => Err(ValueParserError::new(format!("Invalid id strategy {}.", s)))
        }
    }
}

#[derive(Debug, Copy, Clone)]
pub enum ScalarListStrategy {
    Embedded,
    Relation
}

impl FromStr for ScalarListStrategy {
    type Err = ValueParserError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "EMBEDDED" => Ok(ScalarListStrategy::Embedded),
            "RELATION" => Ok(ScalarListStrategy::Relation),
            _ => Err(ValueParserError::new(format!("Invalid scalar list strategy {}.", s)))
        }
    }
}

#[derive(Debug)]
pub struct Sequence {
    pub name: String, 
    pub initial_value: i32,
    pub allocation_size: i32
}

impl WithName for Sequence {
    fn name(&self) -> &String { &self.name }
    fn set_name(&mut self, name: &String) { self.name = name.clone() }
}

#[derive(Debug)]
pub struct Field {
    pub name: String,
    pub arity: FieldArity,
    pub field_type: FieldType,
    pub database_name: Option<String>,
    pub default_value: Option<Value>,
    pub is_unique: bool,
    pub is_id: bool,
    pub id_strategy: Option<IdStrategy>,
    // TODO: Not sure if a sequence should be a member of field.
    pub id_sequence: Option<Sequence>,
    pub scalar_list_strategy: Option<ScalarListStrategy>,
    pub comments: Vec<Comment>
}

impl WithName for Field {
    fn name(&self) -> &String { &self.name }
    fn set_name(&mut self, name: &String) { self.name = name.clone() }
}

impl WithDatabaseName for Field {
    fn database_name(&self) -> &Option<String> { &self.database_name }
    fn set_database_name(&mut self, database_name: &Option<String>) { self.database_name = database_name.clone() }
}

impl Field {
    fn new(name: &String, field_type: &FieldType) -> Field {
        Field {
            name: name.clone(),
            arity: FieldArity::Required,
            field_type: field_type.clone(),
            database_name: None,
            default_value: None,
            is_unique: false,
            is_id: false,
            id_strategy: None,
            id_sequence: None,
            scalar_list_strategy: None,
            comments: vec![]
        }
    }
}

#[derive(Debug)]
pub struct Enum { 
    pub name: String,
    pub values: Vec<String>,
    pub comments: Vec<Comment>
}

impl WithName for Enum {
    fn name(&self) -> &String { &self.name }
    fn set_name(&mut self, name: &String) { self.name = name.clone() }
}


#[derive(Debug)]
pub struct Model {
    pub name: String,
    pub fields: Vec<Field>,
    pub comments: Vec<Comment>,
    pub database_name: Option<String>,
    pub is_embedded: bool
}

impl Model {
    fn new(name: &String) -> Model {
        Model {
            name: name.clone(),
            fields: vec![],
            comments: vec![],
            database_name: None,
            is_embedded: false
        }
    }
}

impl WithName for Model {
    fn name(&self) -> &String { &self.name }
    fn set_name(&mut self, name: &String) { self.name = name.clone() }
}

impl WithDatabaseName for Model {
    fn database_name(&self) -> &Option<String> { &self.database_name }
    fn set_database_name(&mut self, database_name: &Option<String>) { self.database_name = database_name.clone() }
}

#[derive(Debug)]
pub enum ModelOrEnum {
    Enum(Enum),
    Model(Model)
}

#[derive(Debug)]
pub struct Schema {
    pub models: Vec<ModelOrEnum>,
    pub comments: Vec<Comment>
}

impl Schema {
    fn new() -> Schema {
        Schema {
            models: vec![],
            comments: vec![]
        }
    }
}