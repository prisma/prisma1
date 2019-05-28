// use super::DataModelWarningOrError;
// use crate::commands::command::MigrationCommand;
// use migration_connector::steps::*;

// pub struct StartMigrationCommand {
//     input: StartMigrationInput,
// }

// impl MigrationCommand for StartMigrationCommand {
//     type Input = StartMigrationInput;
//     type Output = StartMigrationOutput;

//     fn new(
//         input: StartMigrationInput,
//         connector: Box<MigrationConnector<DatabaseMigrationStep = DatabaseMigrationStepExt>>,
//     ) -> Box<Self> {
//         Box::new(StartMigrationCommand { input })
//     }

//     fn execute(&self) -> StartMigrationOutput {
//         println!("{:?}", self.input);
//         let response = StartMigrationOutput {
//             data_model_errors: vec![],
//             data_model_warnings: vec![],
//             general_errors: vec![],
//         };
//         response
//     }
// }

// #[derive(Debug, Deserialize)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct StartMigrationInput {
//     pub project: String,
//     pub steps: Vec<MigrationStep>,
//     pub data_model: String,
// }

// #[derive(Debug, Serialize)]
// #[serde(rename_all = "camelCase")]
// pub struct StartMigrationOutput {
//     pub data_model_errors: Vec<DataModelWarningOrError>,
//     pub data_model_warnings: Vec<DataModelWarningOrError>,
//     pub general_errors: Vec<String>,
// }
