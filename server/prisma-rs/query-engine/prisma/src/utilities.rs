use crate::{error::PrismaError, PrismaResult};
use std::env;

pub fn get_env(key: &str) -> PrismaResult<String> {
  env::var(key)
    .map_err(|_| PrismaError::ConfigurationError(format!("Environment variable {} required but not found", key)))
}
