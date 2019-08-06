use super::MigrationTask;
use crate::{
    commands::{InferMigrationStepsInput, MigrationStepsResultOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct InferMigrationStepsTask {
    engine: Arc<dyn GenericApi>,
    input: InferMigrationStepsInput,
}

impl<'a> MigrationTask<'a> for InferMigrationStepsTask {
    type Input = InferMigrationStepsInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for InferMigrationStepsTask {
    type Output = MigrationStepsResultOutput;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.infer_migration_steps(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<MigrationStepsResultOutput>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
