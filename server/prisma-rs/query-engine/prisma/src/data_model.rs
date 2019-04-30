use crate::{utilities, PrismaError, PrismaResult};
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

/// Loads and builds the internal schema from the data model
pub fn load(db_name: String) -> PrismaResult<SchemaRef> {
    let data_model_json = load_string()?;
    Ok(serde_json::from_str::<SchemaTemplate>(&data_model_json)?.build(db_name))
}

/// Loads the config as unparsed json string.
/// Attempts to resolve the data model from env and from file, see `load_from_env` and `load_from_file`.
pub fn load_string() -> PrismaResult<String> {
    load_from_env().or_else(|_| load_from_file()).map_err(|err| {
        PrismaError::ConfigurationError(format!("Unable to resolve Prisma data model. Last error: {}", err))
    })
}

/// Attempts to resolve the internal data model from an env var.
/// Note that the content of the env var has to be base64 encoded JSON.
pub fn load_from_env() -> PrismaResult<String> {
    debug!("Trying to load data model from env...");

    utilities::get_env("PRISMA_INTERNAL_DATA_MODEL_JSON").and_then(|schema| {
        let bytes = base64::decode(&schema)?;
        let schema_json = String::from_utf8(bytes)?;

        debug!("Loaded schema from env.");
        Ok(schema_json)
    })
}

/// Attempts to resolve the internal data model from a Prisma SDL (DataModel) file.
/// The contents of that file are processed by the external schema inferrer (until we have a Rust equivalent),
/// which produces the internal data model JSON string.
pub fn load_from_file() -> PrismaResult<String> {
    debug!("Trying to load data model from file...");
    let data_model = load_sdl_string()?;

    #[derive(Serialize)]
    #[serde(rename_all = "camelCase")]
    struct SchemaInferrerJson {
        data_model: String,
    }

    let schema_inferrer = utilities::get_env("SCHEMA_INFERRER_PATH")?;
    let mut child = Command::new(schema_inferrer)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()?;

    let child_in = child.stdin.as_mut().unwrap();
    let json = serde_json::to_string(&SchemaInferrerJson { data_model })?;

    child_in.write_all(json.as_bytes()).expect("Failed to write to stdin");

    let output = child.wait_with_output()?;
    let inferred = String::from_utf8(output.stdout)?;

    debug!(
        "Loaded data model from file: {}.",
        utilities::get_env("PRISMA_DATA_MODEL_PATH")?
    );
    Ok(inferred)
}

pub fn load_sdl_string() -> PrismaResult<String> {
    let path = utilities::get_env("PRISMA_DATA_MODEL_PATH")?;
    let mut f = File::open(&path)?;
    let mut data_model = String::new();

    f.read_to_string(&mut data_model)?;
    Ok(data_model)
}
