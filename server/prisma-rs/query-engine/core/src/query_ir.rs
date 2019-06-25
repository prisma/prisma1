///! Intermediate representation of the input document that is used by the query engine to build
///! query ASTs and validate the incoming data.
///!
///! Helps decoupling the incoming protocol layer from the query engine, making the query engine
///! agnostic to whatever protocol is implemented on upper layers.
use std::collections::BTreeMap;

pub struct QueryDocument {
    pub operations: Vec<Operation>,
}

pub enum Operation {
    Read(ReadOperation),
    Write(WriteOperation),
}

pub struct ReadOperation {
    pub name: Option<String>,
    pub selections: Vec<Selection>,
}

pub struct WriteOperation {
    pub name: Option<String>,
    pub selections: Vec<Selection>,
}

pub struct Selection {
    pub name: String,
    pub alias: Option<String>,
    pub arguments: Vec<(String, QueryValue)>,
    pub sub_selections: Vec<Selection>,
}

pub enum QueryValue {
    Int(i64),
    Float(f64),
    String(String),
    Boolean(bool),
    Null,
    Enum(String),
    List(Vec<QueryValue>),
    Object(BTreeMap<String, QueryValue>),
}
