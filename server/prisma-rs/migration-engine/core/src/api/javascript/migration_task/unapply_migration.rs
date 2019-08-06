use super::MigrationTask;
use crate::{
    commands::{UnapplyMigrationInput, UnapplyMigrationOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct UnapplyMigrationTask {
    engine: Arc<dyn GenericApi>,
    input: UnapplyMigrationInput,
}

impl<'a> MigrationTask<'a> for UnapplyMigrationTask {
    type Input = UnapplyMigrationInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for UnapplyMigrationTask {
    type Output = UnapplyMigrationOutput;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.unapply_migration(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Self::Output>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
