use super::MigrationTask;
use crate::{
    commands::{CalculateDatabaseStepsInput, MigrationStepsResultOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct CalculateDatabaseStepsTask {
    engine: Arc<dyn GenericApi>,
    input: CalculateDatabaseStepsInput,
}

impl<'a> MigrationTask<'a> for CalculateDatabaseStepsTask {
    type Input = CalculateDatabaseStepsInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for CalculateDatabaseStepsTask {
    type Output = MigrationStepsResultOutput;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.calculate_database_steps(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Self::Output>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
