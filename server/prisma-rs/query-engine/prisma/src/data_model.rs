use crate::{utilities, PrismaError, PrismaResult};
use graphql_parser::query;
use prisma_models::{InternalDataModelRef, InternalDataModelTemplate};
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

impl Validatable for InternalDataModelRef {
    fn validate(&self, _: &query::Document) -> Result<(), ValidationError> {
        // It's not really ok 😭
        Ok(())
    }
}

/// Loads and builds the internal data model from the data model JSON.
pub fn load(db_name: String) -> PrismaResult<InternalDataModelRef> {
    let data_model_json = load_string()?;
    Ok(serde_json::from_str::<InternalDataModelTemplate>(&data_model_json)?.build(db_name))
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

    utilities::get_env("PRISMA_INTERNAL_DATA_MODEL_JSON").and_then(|internal_data_model| {
        let bytes = base64::decode(&internal_data_model)?;
        let internal_data_model_json = String::from_utf8(bytes)?;

        debug!("Loaded data model from env.");
        Ok(internal_data_model_json)
    })
}

/// Attempts to resolve the internal data model from a Prisma SDL (DataModel) file.
/// The contents of that file are processed by the external schema (data model) inferrer (until we have a Rust equivalent),
/// which produces the internal data model JSON string.
pub fn load_from_file() -> PrismaResult<String> {
    debug!("Trying to load internal data model from file...");
    let data_model = load_sdl_string()?;

    #[derive(Serialize)]
    #[serde(rename_all = "camelCase")]
    struct InternalDataModelInferrerJson {
        data_model: String,
    }

    let internal_data_model_inferrer = utilities::get_env("SCHEMA_INFERRER_PATH")?;
    let mut child = Command::new(internal_data_model_inferrer)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()?;

    let child_in = child.stdin.as_mut().unwrap();
    let json = serde_json::to_string(&InternalDataModelInferrerJson { data_model })?;

    child_in.write_all(json.as_bytes()).expect("Failed to write to stdin");

    let output = child.wait_with_output()?;
    let inferred = String::from_utf8(output.stdout)?;

    debug!(
        "Loaded internal data model from file: {}.",
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
