use super::MigrationTask;
use crate::{
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct ResetTask {
    engine: Arc<dyn GenericApi>,
    input: serde_json::Value,
}

impl<'a> MigrationTask<'a> for ResetTask {
    type Input = serde_json::Value;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for ResetTask {
    type Output = serde_json::Value;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.reset(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Self::Output>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
