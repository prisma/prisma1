mod apply_migration;
mod calculate_database_steps;
mod calculate_datamodel;
mod infer_migration_steps;
mod list_migrations;
mod migration_progress;
mod reset;
mod unapply_migration;

use crate::api::GenericApi;
use neon::prelude::Task;
use serde::de::DeserializeOwned;
use std::sync::Arc;

pub use apply_migration::*;
pub use calculate_database_steps::*;
pub use calculate_datamodel::*;
pub use infer_migration_steps::*;
pub use list_migrations::*;
pub use migration_progress::*;
pub use reset::*;
pub use unapply_migration::*;

pub trait MigrationTask<'a>: Task {
    type Input: DeserializeOwned + 'a;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self;
}
