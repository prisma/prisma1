use super::DataModelWarningOrError;
use crate::commands::command::MigrationCommand;
use crate::migration::migration_steps_inferrer::{
    MigrationStepsInferrer, MigrationStepsInferrerImpl,
};
use nullable::Nullable::*;
use crate::steps::*;

pub struct SuggestMigrationStepsCommand {
    input: SuggestMigrationStepsInput,
}

impl MigrationCommand for SuggestMigrationStepsCommand {
    type Input = SuggestMigrationStepsInput;
    type Output = SuggestMigrationStepsOutput;

    fn new(input: Self::Input) -> Box<Self> {
        Box::new(SuggestMigrationStepsCommand { input })
    }

    fn execute(&self) -> Self::Output {
        let inferrer = MigrationStepsInferrerImpl;
        //        inferrer.infer("sjka0");
        let warning = DataModelWarningOrError {
            tpe: "Blog".to_owned(),
            field: Some("title".to_owned()),
            message: "This is danger".to_owned(),
        };
        let steps = vec![
            MigrationStep::CreateModel(CreateModel {
                name: "Blog".to_owned(),
                db_name: None,
                embedded: None,
            }),
            MigrationStep::UpdateModel(UpdateModel {
                name: "Blog".to_owned(),
                new_name: None,
                db_name: Some(Null),
                embedded: Some(true),
            }),
            MigrationStep::DeleteModel(DeleteModel {
                name: "Post".to_owned(),
            }),
        ];

        SuggestMigrationStepsOutput {
            steps: steps,
            errors: vec![],
            warnings: vec![warning],
        }
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct SuggestMigrationStepsInput {
    pub project: String,
    pub data_model: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SuggestMigrationStepsOutput {
    pub steps: Vec<MigrationStep>,
    pub errors: Vec<DataModelWarningOrError>,
    pub warnings: Vec<DataModelWarningOrError>,
}
