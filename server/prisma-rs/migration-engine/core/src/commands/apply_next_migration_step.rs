// use crate::commands::command::MigrationCommand;
// use chrono::prelude::*;

// pub struct ApplyNextMigrationStepCommand {
//     input: ApplyNextMigrationStepInput,
// }

// impl MigrationCommand for ApplyNextMigrationStepCommand {
//     type Input = ApplyNextMigrationStepInput;
//     type Output = ApplyNextMigrationStepOutput;

//     fn new(input: Self::Input) -> Box<Self> {
//         Box::new(ApplyNextMigrationStepCommand { input })
//     }

//     fn execute(&self) -> Self::Output {
//         println!("{:?}", self.input);
//         let response = ApplyNextMigrationStepOutput {
//             status: MigrationStatus::InProgress,
//             steps: 3,
//             applied: 2,
//             rolled_back: 0,
//             errors: vec![],
//             started_at: Utc::now(),
//             updated_at: Utc::now(),
//         };
//         response
//     }
// }

// #[derive(Debug, Deserialize)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct ApplyNextMigrationStepInput {
//     pub project: String,
// }

// #[derive(Debug, Serialize)]
// #[serde(rename_all = "camelCase")]
// pub struct ApplyNextMigrationStepOutput {
//     pub status: MigrationStatus,
//     pub steps: i32,
//     pub applied: i32,
//     pub rolled_back: i32,
//     pub errors: Vec<String>,
//     pub started_at: DateTime<Utc>,
//     pub updated_at: DateTime<Utc>,
// }

// // TODO: use the one defined in the connector interface instead
// #[derive(Debug, Serialize)]
// pub enum MigrationStatus {
//     Pending,
//     InProgress,
//     Success,
//     RollingBack,
//     RollbackSuccess,
//     RollbackFailure,
// }
