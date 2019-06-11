use crate::{utilities, PrismaError, PrismaResult};
use datamodel::{Datamodel, Source};
use prisma_models::{DatamodelConverter, InternalDataModelTemplate};
use serde::Serialize;
use serde_json;
use std::{
    fs::File,
    io::{Read, Write},
    process::{Command, Stdio},
};

/// Wrapper type to unclutter the interface
pub struct DatamodelV2Components {
    pub datamodel: Datamodel,
    pub data_sources: Vec<Box<dyn Source>>,
}

/// Private helper trait for operations on PrismaResult<Option<T>>.
/// PrismaResult<Option<T>> expresses 3 states:
/// - [Ok(Some)] Existing.
/// - [Ok(None)] Not existing / not set.
/// - [Err]      Existing, but processing error.
///
/// Err cases abort chaining, the other allow for further chaining.
trait PrismaResultOption<T> {
    fn inner_map<F, U>(self, f: F) -> PrismaResult<Option<U>>
    where
        F: Fn(T) -> PrismaResult<Option<U>>;

    fn inner_or_else<F>(self, f: F) -> PrismaResult<Option<T>>
    where
        F: Fn() -> PrismaResult<Option<T>>;

    fn on_success<F>(self, f: F) -> PrismaResult<Option<T>>
    where
        F: Fn() -> ();
}

impl<T> PrismaResultOption<T> for PrismaResult<Option<T>> {
    fn inner_map<F, U>(self, f: F) -> PrismaResult<Option<U>>
    where
        F: Fn(T) -> PrismaResult<Option<U>>,
    {
        match self? {
            None => Ok(None),
            Some(x) => f(x),
        }
    }

    fn inner_or_else<F>(self, f: F) -> PrismaResult<Option<T>>
    where
        F: Fn() -> PrismaResult<Option<T>>,
    {
        match self? {
            None => f(),
            x => Ok(x),
        }
    }

    fn on_success<F>(self, f: F) -> PrismaResult<Option<T>>
    where
        F: Fn() -> (),
    {
        self.inner_map(|x| {
            f();
            Ok(Some(x))
        })
    }
}

/// Loads data model components (SDL string, v2 data model, internal data model).
///
/// The precendence order is:
/// 1. The datamodel loading is bypassed by providing a pre-build internal data model template
///    via PRISMA_INTERNAL_DATA_MODEL_JSON. This is only intended to be used by integration tests or in
///    rare cases where we don't want to compute the data model.
/// -> Returns (None, None, InternalDataModelTemplate)
///
/// 2. The v2 data model is provided either as file (PRISMA_DML_PATH) or as string in the env (PRISMA_DML).
/// -> Returns (None, Some(DatamodelV2Components), InternalDataModelTemplate)
///
/// 3. The v1 data model is provided either as file (PRISMA_SDL_PATH) or as string in the env (PRISMA_SDL).
/// -> Returns (Some(String), None, InternalDataModelTemplate)
///
/// Encountered Err results abort the chain, else Nones are chained until a Some is encountered in the load order.
pub fn load_data_model_components(
) -> PrismaResult<(Option<String>, Option<DatamodelV2Components>, InternalDataModelTemplate)> {
    // Load data model in order of precedence.
    let triple = match load_v11_from_env_json()? {
        Some(template) => (None, None, template),
        None => match load_datamodel_v2()? {
            Some(v2components) => {
                let template = DatamodelConverter::convert(&v2components.datamodel);
                (None, Some(v2components), template)
            }
            None => match load_v1_sdl_string()? {
                Some(sdl) => {
                    let inferred = infer_v11_json(sdl.as_ref())?;
                    let template = serde_json::from_str::<InternalDataModelTemplate>(&inferred)?;
                    (Some(sdl), None, template)
                }
                None => {
                    return Err(PrismaError::ConfigurationError(
                        "Unable to load data model (neither v1.1 / v2) from any source.".into(),
                    ))
                }
            },
        },
    };

    Ok(triple)
}

/// Attempts to construct an internal data model template from env.
/// Returns: InternalDataModelTemplate
///     Err      If the env var was found, but loading the template failed.
///     Ok(Some) If the env var was found, and the template loaded successfully.
///     Ok(None) If the env var was not found.
fn load_v11_from_env_json() -> PrismaResult<Option<InternalDataModelTemplate>> {
    debug!("Trying to load internal data model from env...");

    load_string_from_env("PRISMA_INTERNAL_DATA_MODEL_JSON").inner_map(|data_model_json| {
        let parsed = serde_json::from_str::<InternalDataModelTemplate>(&data_model_json)?;
        debug!("Loaded internal data model from env.");
        Ok(Some(parsed))
    })
}

/// Attempts to construct a Prisma v2 datamodel.
/// Returns: DatamodelV2Components
///     Err      If a source for v2 was found, but conversion failed.
///     Ok(Some) If a source for v2 was found, and the conversion suceeded.
///     Ok(None) If no source for a v2 data model was found.
fn load_datamodel_v2() -> PrismaResult<Option<DatamodelV2Components>> {
    debug!("Trying to load v2 data model...");

    load_v2_dml_string().inner_map(|dml_string| match datamodel::parse(&dml_string) {
        Err(errors) => Err(PrismaError::ConversionError(errors, dml_string.clone())),
        Ok(dm) => match datamodel::load_configuration(&dml_string) {
            Err(errors) => Err(PrismaError::ConversionError(errors, dml_string.clone())),
            Ok(configuration) => {
                debug!("Loaded Prisma v2 data model.");
                Ok(Some(DatamodelV2Components {
                    datamodel: dm,
                    data_sources: configuration.datasources,
                }))
            }
        },
    })
}

/// Attempts to load a Prisma DML (datamodel v2) string from either env or file.
/// Env has precedence over file.
fn load_v2_dml_string() -> PrismaResult<Option<String>> {
    load_v2_string_from_env().inner_or_else(|| load_v2_dml_from_file())
}

/// Attempts to load a Prisma DML (datamodel v2) string from env.
fn load_v2_string_from_env() -> PrismaResult<Option<String>> {
    debug!("Trying to load Prisma v2 DML from env...");
    load_string_from_env("PRISMA_DML").on_success(|| debug!("Loaded Prisma v2 DML from env."))
}

/// Attempts to load a Prisma DML (datamodel v2) string from file.
fn load_v2_dml_from_file() -> PrismaResult<Option<String>> {
    debug!("Trying to load Prisma v2 Datamodel from file...");
    load_from_file("PRISMA_DML_PATH").on_success(|| debug!("Loaded Prisma v2 DML from file."))
}

/// Attempts to load a Prisma SDL (datamodel v1.1) string from either env or file.
/// Env has precedence over file.
fn load_v1_sdl_string() -> PrismaResult<Option<String>> {
    debug!("Trying to load Prisma v1.1 Datamodel...");
    load_v11_sdl_from_env()
        .inner_or_else(|| load_v11_sdl_from_file())
        .on_success(|| debug!("Loaded Prisma v1.1 data model."))
}

/// Attempts to load a Prisma SDL (datamodel v1.1) string from env.
fn load_v11_sdl_from_env() -> PrismaResult<Option<String>> {
    debug!("Trying to load Prisma v1.1 SDL from env...");
    load_string_from_env("PRISMA_SDL").on_success(|| debug!("Loaded Prisma v1.1 SDL from env."))
}

/// Attempts to load a Prisma SDL (datamodel v1.1) string from file.
fn load_v11_sdl_from_file() -> PrismaResult<Option<String>> {
    debug!("Trying to load Prisma v1.1 SDL from file...");
    load_from_file("PRISMA_SDL_PATH").on_success(|| debug!("Loaded Prisma v1.1 SDL from file."))
}

/// Attempts to load a string from given env var.
/// The contents of the env var can be base64 encoded if necessary (decoding will be attempted).
/// Returns: Env var contents as string.
///     Err      If the env var was found, the contents were base64 encoded, but loading the base64 into a UTF-8 string failed.
///     Ok(Some) If the env var was found (if it was encoded, it was also decoded successfully).
///     Ok(None) If the env var was not found.
fn load_string_from_env(env_var: &str) -> PrismaResult<Option<String>> {
    match utilities::get_env(env_var).ok() {
        Some(string) => match base64::decode(&string) {
            Ok(bytes) => match String::from_utf8(bytes) {
                Ok(result) => {
                    trace!("Successfully decoded {} from Base64.", env_var);
                    Ok(Some(result))
                }
                Err(err) => {
                    trace!("Error decoding {} from Base64 (invalid UTF-8): {:?}", env_var, err);
                    Err(PrismaError::ConfigurationError("Invalid Base64".into()))
                }
            },
            Err(e) => {
                trace!("Decoding Base64 failed (might not be encoded): {:?}", e);
                Ok(Some(string))
            }
        },
        None => Ok(None),
    }
}

/// Attempts to load a string from a file pointed to by given env var.
/// Returns: File contents as string.
///     Err      If the env var was found, but loading the file failed.
///     Ok(Some) If the env var was found and the file was successfully read.
///     Ok(None) If the env var was not found.
fn load_from_file(env_var: &str) -> PrismaResult<Option<String>> {
    match utilities::get_env(env_var).ok() {
        Some(path) => {
            let mut f = File::open(&path)?;
            let mut sdl = String::new();

            f.read_to_string(&mut sdl)?;
            trace!("Successfully loaded contents of {}", path);

            Ok(Some(sdl))
        }
        None => Ok(None),
    }
}

/// Transforms an SDL string into stringified JSON of the internal data model template.
/// Calls out to an external process. Requires SCHEMA_INFERRER_PATH to be set.
fn infer_v11_json(sdl: &str) -> PrismaResult<String> {
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
