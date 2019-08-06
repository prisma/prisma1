use super::MigrationTask;
use crate::{
    commands::{MigrationProgressInput, MigrationProgressOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct MigrationProgressTask {
    engine: Arc<dyn GenericApi>,
    input: MigrationProgressInput,
}

impl<'a> MigrationTask<'a> for MigrationProgressTask {
    type Input = MigrationProgressInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for MigrationProgressTask {
    type Output = MigrationProgressOutput;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.migration_progress(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Self::Output>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
