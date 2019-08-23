///! Intermediate representation of the input document that is used by the query engine to build
///! query ASTs and validate the incoming data.
///!
///! Helps decoupling the incoming protocol layer from the query engine, i.e. allows the query engine
///! to be agnostic to the actual protocol that is used on upper layers, as long as they translate
///! to this simple intermediate representation.
///!
///! The mapping illustrated with GraphQL (GQL):
///! - There can be multiple queries in one request. Each of those is an `Operation`.
///! - An operation is either reading or writing data (`query`, `mutation` in GQL).
///! - An operation can be named, e.g. `query a { ... }`. This influences the shape of the result as well.
///! - An operation has a number of selections, which designate top level read or writes (GQL fields: `query a { selection1, selection2, ... }`).
///! - A selection can have arguments, can be aliased, and can have a number of sub-selections (identical to GQL).
///! - Arguments contain concrete values and complex subtypes that are parsed and validated by the query builders, and then used for querying data (identical to GQL).
use std::collections::BTreeMap;

#[derive(Debug)]
pub struct QueryDocument {
    pub operations: Vec<Operation>,
}

#[derive(Debug)]
pub enum Operation {
    Read(ReadOperation),
    Write(WriteOperation),
}

#[derive(Debug)]
pub struct ReadOperation {
    pub name: Option<String>,
    pub selections: Vec<Selection>,
}

#[derive(Debug)]
pub struct WriteOperation {
    pub name: Option<String>,
    pub selections: Vec<Selection>,
}

#[derive(Debug)]
pub struct Selection {
    pub name: String,
    pub alias: Option<String>,
    pub arguments: Vec<(String, QueryValue)>,
    pub sub_selections: Vec<Selection>,
}

#[derive(Debug, Clone)]
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
