mod connection_string;
mod explicit;
mod file;

pub use connection_string::ConnectionStringConfig;
pub use explicit::ExplicitConfig;
pub use file::FileConfig;

use crate::{error::Error, PrismaResult};
use serde_yaml;
use std::{collections::BTreeMap, env, fs::File, path::PathBuf};

trait WithMigrations {
    fn migrations(&self) -> Option<bool>;
    fn is_active(&self) -> Option<bool>;

    fn with_migrations(&self) -> bool {
        self.migrations().or_else(|| self.is_active()).unwrap_or(false)
    }
}

pub trait ConnectionLimit {
    fn connection_limit(&self) -> Option<u32>;
    fn pooled(&self) -> Option<bool>;

    fn limit(&self) -> u32 {
        match self.pooled() {
            Some(false) | None => 1,
            _ => self.connection_limit().unwrap_or(1),
        }
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase", untagged)]
pub enum PrismaDatabase {
    Explicit(ExplicitConfig),
    ConnectionString(ConnectionStringConfig),
    File(FileConfig),
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct PrismaConfig {
    pub port: Option<i16>,
    pub management_api_secret: Option<String>,
    pub cluster_address: Option<String>,
    pub rabbit_uri: Option<String>,
    pub enable_management_api: Option<bool>,
    pub databases: BTreeMap<String, PrismaDatabase>,
}

/// Loads the config
pub fn load() -> PrismaResult<PrismaConfig> {
    let config_path: PathBuf = match env::var("PRISMA_CONFIG") {
        Ok(ref p) => {
            let path = PathBuf::from(p);
            if path.exists() {
                Err(Error::ConfigurationError(format!("File {} doesn't exist", p)))
            } else {
                Ok(path)
            }
        }
        Err(_) => match find_config_path() {
            Some(path) => Ok(path),
            None => Err(Error::ConfigurationError("Unable to find Prisma config.".into())),
        },
    }
    .unwrap();

    Ok(config_path.into())
}

pub fn find_config_path() -> Option<PathBuf> {
    let mut bp = PathBuf::new();
    bp.push(std::env::var("PRISMA_CONFIG_PATH").expect("Did you forget to load your env vars? :)"));
    Some(bp)
}

impl From<PathBuf> for PrismaConfig {
    fn from(pb: PathBuf) -> PrismaConfig {
        serde_yaml::from_reader(File::open(pb).unwrap()).unwrap()
    }
}
