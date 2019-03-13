use graphql_parser::{self as gql, query, schema::Document};
use std::env;
use std::fs::File;
use std::io::Read;
use std::ops::Deref;

pub enum ValidationError {
    EverythingIsBroken,
}

pub trait Validatable {
    fn validate(&self, doc: query::Document) -> Result<(), ValidationError>;
}

#[derive(Debug, Clone, PartialEq)]
pub struct PrismaSchema {
    inner: Document,
}

impl Validatable for PrismaSchema {
    fn validate(&self, doc: query::Document) -> Result<(), ValidationError> {
        // It's not really ok 😭
        Ok(())
    }
}

impl Deref for PrismaSchema {
    type Target = Document;
    fn deref(&self) -> &Self::Target {
        &self.inner
    }
}

pub fn load_schema() -> Result<PrismaSchema, Box<std::error::Error>> {
    let path = env::var("PRISMA_EXAMPLE_SCHEMA").unwrap();
    let mut f = File::open(path).unwrap();
    let mut s = String::new();
    f.read_to_string(&mut s).unwrap();

    Ok(PrismaSchema {
        inner: gql::parse_schema(&s).unwrap(),
    })
}
