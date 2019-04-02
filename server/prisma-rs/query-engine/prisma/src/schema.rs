use crate::{utilities, PrismaResult};
use graphql_parser::query;
use prisma_models::{SchemaRef, SchemaTemplate};
use serde::Serialize;
use serde_json;
use std::{
    fs::File,
    io::{Read, Write},
    process::{Command, Stdio},
};

pub enum ValidationError {
    #[allow(dead_code)]
    EverythingIsBroken,
    #[allow(dead_code)]
    Problematic(String),
    #[allow(dead_code)]
    Duplicate(String),
}

// todo: Return crate::error errors, removing the above?
pub trait Validatable {
    fn validate(&self, doc: &query::Document) -> Result<(), ValidationError>;
}

impl Validatable for SchemaRef {
    fn validate(&self, _: &query::Document) -> Result<(), ValidationError> {
        // It's not really ok 😭
        Ok(())
    }
}

pub fn load_schema(db_name: String) -> PrismaResult<SchemaRef> {
    let schema_json = load_schema_from_env().or_else(|_| load_datamodel_file())?;
    Ok(serde_json::from_str::<SchemaTemplate>(&schema_json)?.build(db_name))
}

pub fn load_schema_from_env() -> PrismaResult<String> {
    std::env::vars().for_each(|var| println!("{} -> {}", var.0, var.1));
    debug!("Trying to load schema from env...");

    utilities::get_env("PRISMA_SCHEMA_JSON").and_then(|schema| {
        let bytes = base64::decode(&schema)?;
        let schema_json = String::from_utf8(bytes)?;
        debug!("Loaded schema from env.");

        Ok(schema_json)
    })
}

pub fn load_datamodel_file() -> PrismaResult<String> {
    debug!("Trying to load schema from file...");
    let path = utilities::get_env("PRISMA_SCHEMA_PATH")?;
    let mut f = File::open(path)?;
    let mut schema = String::new();

    f.read_to_string(&mut schema)?;

    #[derive(Serialize)]
    #[serde(rename_all = "camelCase")]
    struct SchemaJson {
        data_model: String,
    }

    let schema_inferrer = utilities::get_env("SCHEMA_INFERRER_PATH")?;
    let mut child = Command::new(schema_inferrer)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()?;

    let child_in = child.stdin.as_mut().unwrap();
    let json = serde_json::to_string(&SchemaJson { data_model: schema })?;

    child_in.write_all(json.as_bytes()).expect("Failed to write to stdin");

    let output = child.wait_with_output()?;
    let inferred = String::from_utf8(output.stdout)?;

    debug!("Loaded schema from file.");
    Ok(inferred)
}
