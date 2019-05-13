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

/// Attempts to load the config as unparsed JSON string.
pub fn load_string() -> PrismaResult<String> {
    load_internal_from_env().or_else(|_| load_sdl_string().and_then(|sdl| {
        resolve_internal_data_model_json(sdl)
    })).map_err(|err| {
        PrismaError::ConfigurationError(format!("Unable to construct internal Prisma data model from any source. Last error: {}", err))
    })
}

/// Attempts to resolve the internal data model from an env var.
/// Note that the content of the env var has to be base64 encoded.
/// Returns: Internal data model JSON string.
pub fn load_internal_from_env() -> PrismaResult<String> {
    debug!("Trying to load internal data model from env...");

    utilities::get_env("PRISMA_INTERNAL_DATA_MODEL_JSON").and_then(|internal_data_model_b64| {
        let bytes = base64::decode(&internal_data_model_b64)?;
        let internal_data_model_json = String::from_utf8(bytes)?;

        debug!("Loaded internal data model from env.");
        Ok(internal_data_model_json)
    })
}

// let inferrer = resolve_internal_data_model_json(sdl)?;

/// Attempts to load a Prisma SDL string from either env or file.
pub fn load_sdl_string() -> PrismaResult<String> {
    load_sdl_from_env().or_else(|_| load_sdl_from_file()).map_err(|err| {
        PrismaError::ConfigurationError(format!("Unable to load SDL from any source. Last error: {}", err))
    })
}

/// Attempts to load a Prisma SDL string from env.
/// Note that the content of the env var has to be base64 encoded.
/// Returns: Decoded Prisma SDL string.
fn load_sdl_from_env() -> PrismaResult<String> {
    debug!("Trying to load Prisma SDL from env...");
    utilities::get_env("PRISMA_SDL").and_then(|sdl_b64| {
        let bytes = base64::decode(&sdl_b64)?;
        let sdl = String::from_utf8(bytes)?;

        debug!("Loaded Prisma SDL from env.");
        Ok(sdl)
    })
}

/// Attempts to load a Prisma SDL string from file.
/// Returns: Decoded Prisma SDL string.
pub fn load_sdl_from_file() -> PrismaResult<String> {
    debug!("Trying to load Prisma SDL from file...");

    let path = utilities::get_env("PRISMA_SDL_PATH")?;
    let mut f = File::open(&path)?;
    let mut sdl = String::new();

    f.read_to_string(&mut sdl)?;

    debug!(
        "Loaded Prisma SDL from file: {}.",
        utilities::get_env("PRISMA_SDL_PATH")?
    );

    Ok(sdl)
}

/// Transforms an SDL string into stringified JSON of the internal data model.
fn resolve_internal_data_model_json(sdl: String) -> PrismaResult<String> {
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

    let compacted = sdl.replace('\n', " ");
    let child_in = child.stdin.as_mut().unwrap();
    let json = serde_json::to_string(&InternalDataModelInferrerJson { data_model: compacted })?;

    child_in.write_all(json.as_bytes()).expect("Failed to write to stdin");

    let output = child.wait_with_output()?;
    let inferred = String::from_utf8(output.stdout)?;

    Ok(inferred)
}