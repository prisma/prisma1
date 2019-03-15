use graphql_parser::query;
use prisma_models::{SchemaRef, SchemaTemplate};
use serde::Serialize;
use serde_json;
use std::env;
use std::fs::File;
use std::io::{Read, Write};
use std::process::{Command, Stdio};

pub enum ValidationError {
    EverythingIsBroken,
    Problematic(String),
    Duplicate(String),
}

pub trait Validatable {
    fn validate(&self, doc: &query::Document) -> Result<(), ValidationError>;
}

impl Validatable for SchemaRef {
    fn validate(&self, doc: &query::Document) -> Result<(), ValidationError> {
        // It's not really ok 😭
        Ok(())
    }
}

pub fn load_schema(db_name: String) -> Result<SchemaRef, Box<std::error::Error>> {
    let path = env::var("PRISMA_SCHEMA_PATH")?;
    let mut f = File::open(path)?;
    let mut schema = String::new();

    f.read_to_string(&mut schema)?;

    #[derive(Serialize)]
    #[serde(rename_all = "camelCase")]
    struct SchemaJson {
        data_model: String,
    }

    let schema_inferrer = env::var("SCHEMA_INFERRER_PATH")?;
    let mut child = Command::new(schema_inferrer)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()?;

    let child_in = child.stdin.as_mut().unwrap();
    let json = serde_json::to_string(&SchemaJson { data_model: schema })?;

    child_in.write_all(json.as_bytes()).expect("Failed to write to stdin");

    let output = child.wait_with_output()?;
    let inferred = String::from_utf8(output.stdout)?;

    // FIXME: how can we inject the right db name?
    Ok(serde_json::from_str::<SchemaTemplate>(&inferred)?.build(db_name))
}
