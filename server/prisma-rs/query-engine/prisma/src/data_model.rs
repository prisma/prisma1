use crate::{utilities, PrismaError, PrismaResult};
use graphql_parser::query;
use prisma_models::{DatamodelConverter, InternalDataModelRef, InternalDataModelTemplate};
use serde::Serialize;
use serde_json;
use std::{
    fs::File,
    io::{Read, Write},
    process::{Command, Stdio},
};

// pub enum ValidationError {
//     #[allow(dead_code)]
//     EverythingIsBroken,
//     #[allow(dead_code)]
//     Problematic(String),
//     #[allow(dead_code)]
//     Duplicate(String),
// }

// // todo: Return crate::error errors, removing the above?
// pub trait Validatable {
//     fn validate(&self, doc: &query::Document) -> Result<(), ValidationError>;
// }

// impl Validatable for InternalDataModelRef {
//     fn validate(&self, _: &query::Document) -> Result<(), ValidationError> {
//         // It's not really ok 😭
//         Ok(())
//     }
// }

/// Loads and builds the internal data model
// pub fn load(db_name: String) -> PrismaResult<InternalDataModelRef> {
//     let template = match load_datamodel_v2_from_env() {
//         Ok(template) => template,
//         Err(_) => {
//             let data_model_json = load_v11_json()?;
//             serde_json::from_str::<InternalDataModelTemplate>(&data_model_json)?
//         }
//     };
//     Ok(template.build(db_name))
// }

// fn load_datamodel_v2_from_env() -> PrismaResult<InternalDataModelTemplate> {
//     let datamodel_string = load_v2_dml_string()?;
//     Ok(DatamodelConverter::convert_string(datamodel_string))
// }

/// Attempts to load the config as unparsed JSON string.
fn load_v11_json() -> PrismaResult<String> {
    load_v11_json_from_env()
        .or_else(|_| load_v11_sdl_string().and_then(|sdl| infer_v11_json(sdl)))
        .map_err(|err| {
            PrismaError::ConfigurationError(format!(
                "Unable to construct internal Prisma data model from any source. Last error: {}",
                err
            ))
        })
}

/// Attempts to resolve the internal data model from an env var.
/// Note that the content of the env var has to be base64 encoded.
/// Returns: Internal data model JSON string.
fn load_v11_json_from_env() -> PrismaResult<String> {
    debug!("Trying to load internal data model from env...");

    utilities::get_env("PRISMA_INTERNAL_DATA_MODEL_JSON").and_then(|internal_data_model_b64| {
        let bytes = base64::decode(&internal_data_model_b64)?;
        let internal_data_model_json = String::from_utf8(bytes)?;

        debug!("Loaded internal data model from env.");
        Ok(internal_data_model_json)
    })
}

/// Attempts to load a Prisma SDL (datamodel v1) string from either env or file.
/// Env has precedence over file.
pub fn load_v1_sdl_string() -> PrismaResult<String> {
    load_v11_sdl_from_env()
        .or_else(|_| load_v11_sdl_from_file())
        .map_err(|err| {
            PrismaError::ConfigurationError(format!("Unable to load SDL from any source. Last error: {}", err))
        })
}

/// Attempts to load a Prisma DML (datamodel v2) string from either env or file.
/// Env has precedence over file.
pub fn load_v2_dml_string() -> PrismaResult<String> {
    load_v2_string_from_env()
        .or_else(|_| load_v2_dml_from_file())
        .map_err(|err| {
            PrismaError::ConfigurationError(format!("Unable to load V2 from any source. Last error: {}", err))
        })
}

fn load_v11_sdl_from_env() -> PrismaResult<String> {
    debug!("Trying to load Prisma v11 SDL from env...");
    let sdl_string = load_datamodel_from_env("PRISMA_SDL");

    debug!("Loaded Prisma v11 SDL from env.");
    sdl_string
}

fn load_v2_string_from_env() -> PrismaResult<String> {
    debug!("Trying to load Prisma v2 DML from env...");
    let dml_string = load_datamodel_from_env("PRISMA_DML");

    debug!("Loaded Prisma v2 DML from env.");
    dml_string
}

/// Attempts to load a Prisma Datamodel string from env.
/// Note that the content of the env var can be base64 encoded if necessary.
/// Returns: (Decoded) Prisma Datamodel string from given env var.
fn load_datamodel_from_env(env_var: &str) -> PrismaResult<String> {
    utilities::get_env(env_var).and_then(|sdl_b64| {
        let sdl = match base64::decode(&sdl_b64) {
            Ok(bytes) => {
                trace!("Decoded Datamodel from Base64.");
                String::from_utf8(bytes)?
            }
            Err(e) => {
                trace!("Error decoding Datamodel Base64: {:?}", e);
                sdl_b64
            }
        };

        Ok(sdl)
    })
}

/// Attempts to load a Prisma SDL string from file.
/// Returns: Decoded Prisma SDL string.
pub fn load_v11_sdl_from_file() -> PrismaResult<String> {
    debug!("Trying to load Prisma v11 SDL from file...");
    load_datamodel_from_file("PRISMA_SDL_PATH")
}

pub fn load_v2_dml_from_file() -> PrismaResult<String> {
    debug!("Trying to load Prisma v2 Datamodel from file...");
    load_datamodel_from_file("PRISMA_DML_PATH")
}

pub fn load_datamodel_from_file(env_var: &str) -> PrismaResult<String> {
    let path = utilities::get_env(env_var)?;
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
fn infer_v11_json(sdl: String) -> PrismaResult<String> {
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
