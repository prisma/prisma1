use graphql_parser::{self as gql, query, schema::Document};
use prisma_models::{SchemaRef, SchemaTemplate};
use std::env;
use std::fs::File;
use std::io::{BufReader, BufWriter, Read, Write};
use std::process::{Command, Stdio};

use serde::Serialize;
use serde_json;

pub enum ValidationError {
    EverythingIsBroken,
    Problematic(String),
    Duplicate(String),
}

pub trait Validatable {
    fn validate(&self, doc: &query::Document) -> Result<(), ValidationError>;
}

pub type GraphQlSchema = Document;

impl Validatable for GraphQlSchema {
    fn validate(&self, doc: &query::Document) -> Result<(), ValidationError> {
        // It's not really ok 😭
        Ok(())
    }
}

pub fn load_schema() -> Result<SchemaRef, Box<std::error::Error>> {
    let path = env::var("PRISMA_EXAMPLE_SCHEMA_JSON")?;
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

    let mut child_in = child.stdin.unwrap();
    let mut writer = BufWriter::new(&mut child_in);

    let mut child_out = child.stdout.unwrap();
    let mut reader = BufReader::new(&mut child_out);

    let json = serde_json::to_string(&SchemaJson { data_model: schema })?;
    writer.write_all(json.as_bytes()).expect("Failed to write to stdin");

    let mut inferred = String::new();
    reader.read_to_string(&mut inferred)?;

    Ok(serde_json::from_str::<SchemaTemplate>(&inferred)?.build("".into()))
}
