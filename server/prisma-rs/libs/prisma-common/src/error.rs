use failure::{Error, Fail};
use std::io;

#[derive(Debug, Fail)]
pub enum CommonError {
    #[fail(display = "Failed to parse the config YAML")]
    YamlError(Error),
    #[fail(display = "Unable to find Prisma config.")]
    ConfigurationError,
    #[fail(display = "{}", _0)]
    IoError(Error),
}

impl From<io::Error> for CommonError {
    fn from(e: io::Error) -> CommonError {
        CommonError::IoError(e.into())
    }
}

impl From<serde_yaml::Error> for CommonError {
    fn from(e: serde_yaml::Error) -> CommonError {
        CommonError::YamlError(e.into())
    }
}
