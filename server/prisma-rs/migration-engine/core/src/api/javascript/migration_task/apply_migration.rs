use super::MigrationTask;
use crate::{
    commands::{ApplyMigrationInput, MigrationStepsResultOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct ApplyMigrationTask {
    engine: Arc<dyn GenericApi>,
    input: ApplyMigrationInput,
}

impl<'a> MigrationTask<'a> for ApplyMigrationTask {
    type Input = ApplyMigrationInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for ApplyMigrationTask {
    type Output = MigrationStepsResultOutput;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.apply_migration(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Self::Output>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
