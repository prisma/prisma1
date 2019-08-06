use super::MigrationTask;
use crate::{
    commands::{CalculateDatamodelInput, CalculateDatamodelOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct CalculateDatamodelTask {
    engine: Arc<dyn GenericApi>,
    input: CalculateDatamodelInput,
}

impl<'a> MigrationTask<'a> for CalculateDatamodelTask {
    type Input = CalculateDatamodelInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for CalculateDatamodelTask {
    type Output = CalculateDatamodelOutput;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.calculate_datamodel(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Self::Output>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
