use super::MigrationTask;
use crate::{
    commands::{ListMigrationStepsInput, ListMigrationStepsOutput},
    api::GenericApi,
};
use neon::prelude::{JsResult, JsValue, Task, TaskContext};
use std::sync::Arc;

pub struct ListMigrationStepsTask {
    engine: Arc<dyn GenericApi>,
    input: ListMigrationStepsInput,
}

impl<'a> MigrationTask<'a> for ListMigrationStepsTask {
    type Input = ListMigrationStepsInput;

    fn create(engine: Arc<dyn GenericApi>, input: Self::Input) -> Self {
        Self { engine, input }
    }
}

impl Task for ListMigrationStepsTask {
    type Output = Vec<ListMigrationStepsOutput>;
    type Error = crate::error::Error;
    type JsEvent = JsValue;

    fn perform(&self) -> crate::Result<Self::Output> {
        self.engine.list_migrations(&self.input)
    }

    fn complete<'a>(
        self,
        mut cx: TaskContext<'a>,
        result: crate::Result<Vec<ListMigrationStepsOutput>>,
    ) -> JsResult<JsValue> {
        Ok(neon_serde::to_value(&mut cx, &result.unwrap())?)
    }
}
