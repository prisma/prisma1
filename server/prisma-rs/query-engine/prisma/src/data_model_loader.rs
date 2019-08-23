use std::{fs::File, io::Read};

use serde::Deserialize;
use serde_json;

use datamodel::{Datamodel, Source};
use prisma_models::{DatamodelConverter, InternalDataModelTemplate};

use crate::{utilities, PrismaError, PrismaResult};

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

/// Loads data model components for the v2 data model.
/// The v2 data model is provided either as file (PRISMA_DML_PATH) or as string in the env (PRISMA_DML).
pub fn load_data_model_components() -> PrismaResult<(DatamodelV2Components, InternalDataModelTemplate)> {
    // Load data model in order of precedence.
    match load_datamodel_v2()? {
        Some(v2components) => {
            let template = DatamodelConverter::convert(&v2components.datamodel);
            Ok((v2components, template))
        }
        None => {
            return Err(PrismaError::ConfigurationError(
                "Unable to load data model v2 from any source.".into(),
            ))
        }
    }
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
        Ok(dm) => load_configuration(&dml_string).map(|configuration| {
            debug!("Loaded Prisma v2 data model.");
            Some(DatamodelV2Components {
                datamodel: dm,
                data_sources: configuration.datasources,
            })
        }),
    })
}

pub fn load_configuration(dml_string: &str) -> PrismaResult<datamodel::Configuration> {
    let datasource_overwrites_string =
        load_string_from_env("OVERWRITE_DATASOURCES")?.unwrap_or_else(|| r#"[]"#.to_string());
    let datasource_overwrites: Vec<SourceOverride> = serde_json::from_str(&datasource_overwrites_string)?;

    match datamodel::load_configuration(&dml_string) {
        Err(errors) => Err(PrismaError::ConversionError(errors, dml_string.to_string())),
        Ok(mut configuration) => {
            for datasource_override in datasource_overwrites {
                for datasource in &mut configuration.datasources {
                    if &datasource_override.name == datasource.name() {
                        debug!(
                            "overwriting datasource {} with url {}",
                            &datasource_override.name, &datasource_override.url
                        );
                        datasource.set_url(&datasource_override.url);
                    }
                }
            }
            Ok(configuration)
        }
    }
}

#[derive(Deserialize)]
struct SourceOverride {
    name: String,
    url: String,
}

/// Attempts to load a Prisma DML (datamodel v2) string from either env or file.
/// Env has precedence over file.
fn load_v2_dml_string() -> PrismaResult<Option<String>> {
    load_v2_string_from_env().inner_or_else(load_v2_dml_from_file)
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
