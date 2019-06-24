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
    name: Option<String>,
    selections: Vec<Selection>,
}

pub struct WriteOperation {
    name: Option<String>,
    selections: Vec<Selection>,
}

pub struct Selection {
    name: String,
    arguments: Vec<(String, QueryValue)>,
    sub_selections: Vec<Selection>,
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
