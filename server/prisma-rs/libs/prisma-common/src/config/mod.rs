mod connection_string;
mod explicit;
mod file;

use crate::error::CommonError;
pub use connection_string::ConnectionStringConfig;
pub use explicit::ExplicitConfig;
pub use file::FileConfig;

use serde_yaml;
use std::{collections::BTreeMap, env, fs::File, io::prelude::*, path::PathBuf};

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

impl PrismaDatabase {
    pub fn db_name(&self) -> Option<String> {
        match self {
            PrismaDatabase::Explicit(config) => config.database.clone(),
            PrismaDatabase::ConnectionString(config) => config.database.clone(),
            PrismaDatabase::File(config) => Some(config.db_name()),
        }
    }
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct PrismaConfig {
    pub port: u16,
    pub management_api_secret: Option<String>,
    pub cluster_address: Option<String>,
    pub rabbit_uri: Option<String>,
    pub enable_management_api: Option<bool>,
    pub databases: BTreeMap<String, PrismaDatabase>,
}

/// Loads the config
pub fn load() -> Result<PrismaConfig, CommonError> {
    let config: String = match env::var("PRISMA_CONFIG") {
        Ok(c) => c,
        Err(_) => match find_config_path() {
            Some(path) => {
                let mut f = File::open(path)?;
                let mut contents = String::new();
                f.read_to_string(&mut contents)?;
                contents
            }
            None => return Err(CommonError::ConfigurationError),
        },
    };

    Ok(serde_yaml::from_str(&config).expect("Unable to parse YML config."))
}

/// Attempts to find a valid Prisma config either via env var or file discovery.
pub fn find_config_path() -> Option<PathBuf> {
    match std::env::var("PRISMA_CONFIG_PATH") {
        Ok(path) => Some(PathBuf::from(path)),
        Err(_) => {
            let mut path = std::env::current_dir().expect("Couldn't resolve current working directory");
            path.push("prisma.yml");

            if path.exists() && path.is_file() {
                Some(path)
            } else {
                None
            }
        }
    }
}
