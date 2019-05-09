use crate::commands::apply_next_migration_step::ApplyNextMigrationStepCommand;
use crate::commands::command::MigrationCommand;
use crate::commands::start_migration::StartMigrationCommand;
use crate::commands::suggest_migration_step::SuggestMigrationStepsCommand;
use jsonrpc_core;
use jsonrpc_core::IoHandler;
use jsonrpc_core::*;
use std::io;

pub struct RpcApi {
    io_handler: jsonrpc_core::IoHandler<()>,
}

impl RpcApi {
    pub fn new() -> RpcApi {
        let mut rpc_api = RpcApi {
            io_handler: IoHandler::new(),
        };
        rpc_api.add_command_handler::<SuggestMigrationStepsCommand>("suggestMigrationSteps");
        rpc_api.add_command_handler::<StartMigrationCommand>("startMigration");
        rpc_api.add_command_handler::<ApplyNextMigrationStepCommand>("applyNextMigrationStep");
        rpc_api
    }

    fn add_command_handler<T: MigrationCommand>(&mut self, name: &str) {
        self.io_handler.add_method(name, |params: Params| {
            let input: T::Input = params.parse()?;
            let cmd = T::new(input);
            let response_json = serde_json::to_value(&cmd.execute()).unwrap();
            Ok(response_json)
        });
    }

    pub fn handle(self) {
        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        let response = self.io_handler.handle_request_sync(&input).unwrap();
        println!("{}", response);
    }
}

//
//impl From<serde_json::Error> for jsonrpc_core::types::error::Error {
//    fn from(serdeError: serde_json::Error) -> Self {
//        jsonrpc_core::types::error::Error {
//            code: ErrorCode::InternalError,
//            message: "boo".to_owned(),
//            data: None,
//        }
//    }
//}
