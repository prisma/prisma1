mod connection_string;
mod explicit;
mod file;

pub use connection_string::ConnectionStringConfig;
pub use explicit::ExplicitConfig;
pub use file::FileConfig;

use crate::error::CommonError;
use datamodel::Source;
use serde_yaml;
use std::{
    collections::{BTreeMap, HashMap},
    convert::TryInto,
    env,
    fs::File,
    io::prelude::*,
    path::PathBuf,
};

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
    pub fn connector(&self) -> &str {
        match self {
            PrismaDatabase::Explicit(config) => &config.connector,
            PrismaDatabase::ConnectionString(config) => &config.connector,
            PrismaDatabase::File(config) => &config.connector,
        }
    }

    pub fn db_name(&self) -> Option<String> {
        match self {
            PrismaDatabase::Explicit(config) => config.database.clone(),
            PrismaDatabase::ConnectionString(config) => config.database.clone(),
            PrismaDatabase::File(config) => Some(config.db_name()),
        }
    }

    pub fn schema(&self) -> Option<String> {
        match self {
            PrismaDatabase::Explicit(config) => config.schema.clone(),
            PrismaDatabase::ConnectionString(config) => config.schema.clone(),
            PrismaDatabase::File(config) => config.schema.clone(),
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

/// Allows a legacy config to be transformed into the new data sources format.
/// WIP: Default is not yet supported in data sources.
impl TryInto<Vec<Box<dyn Source>>> for PrismaConfig {
    type Error = CommonError;

    fn try_into(self) -> Result<Vec<Box<dyn Source>>, Self::Error> {
        self.databases
            .into_iter()
            .map(|(name, db)| match db {
                #[cfg(feature = "sql")]
                PrismaDatabase::File(ref config) if config.connector == "sqlite-native" => {
                    let path = config.database_file;
                    let ospath = PathBuf::from(&path);
                    if ospath.exists() && !ospath.is_dir() {
                        let dummy = vec![];
                        let source = SqliteSourceDefinition::new().create(
                            &name,
                            format!("file:{}", path),
                            &Arguments::empty(&dummy),
                        );

                        Ok(source)
                    } else {
                        Err(CommonError::ConfigurationError("Configuration error: Sqlite file configuration found, but path either doesn't exist or doens't point to a file.".into()));
                    }
                }

                #[cfg(feature = "sql")]
                config if config.connector() == "postgres-native" => unimplemented!(),

                #[cfg(feature = "sql")]
                config if config.connector() == "mysql-native" => unimplemented!(),

                _ => Err(CommonError::ConfigurationError(format!("Database connector for configuration key {} is not supported.", name))),
            })
            .collect::<Vec<Result<Box<dyn Source>, Self::Error>>>()
            .into_iter()
            .collect()
    }
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
            None => return Err(CommonError::ConfigurationError("Unable to find Prisma config".into())),
        },
    };

    let config = substitute_env_vars(config)?;
    Ok(serde_yaml::from_str(&config.replace("\\n", "\n")).expect("Unable to parse YML config."))
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

fn substitute_env_vars(cfg_string: String) -> Result<String, CommonError> {
    let matcher = regex::Regex::new(r"\$\{(.*)\}").unwrap();

    // Collect all env vars first.
    let matches: Vec<String> = matcher
        .captures_iter(cfg_string.as_ref())
        .map(|capture| capture[1].into())
        .collect();

    // Resolve all env vars, unresolved ones will be None in the map
    let resolved_env: HashMap<String, Option<String>> = matches
        .into_iter()
        .map(|m| {
            let val = std::env::var(&m).ok();
            (m, val)
        })
        .collect();

    // Collect all unresolved env vars
    let unresolved_env: Vec<&str> = resolved_env
        .iter()
        .filter_map(|m| if let None = m.1 { Some(m.0.as_ref()) } else { None })
        .collect();

    // Validate that all env vars can be resolved
    if !unresolved_env.is_empty() {
        return Err(CommonError::ConfigurationError(format!(
            "Unresolved env vars in configuration: {}",
            unresolved_env.join(", ")
        )));
    }

    // Replace all occurrences in the config. We can safely unwrap here as we validated already.
    let result = matcher.replace(cfg_string.as_ref(), |caps: &regex::Captures| {
        let env_key = &caps[1];
        resolved_env.get(env_key).unwrap().as_ref().unwrap()
    });

    Ok(result.into())
}
