use crate::commands::*;
use crate::migration_engine::*;
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
        rpc_api.add_command_handler::<InferMigrationStepsCommand>("inferMigrationSteps");
        rpc_api.add_command_handler::<ListMigrationStepsCommand>("listMigrations");
        rpc_api.add_command_handler::<MigrationProgressCommand>("migrationProgress");
        rpc_api.add_command_handler::<ApplyMigrationCommand>("applyMigration");
        rpc_api.add_command_handler::<UnapplyMigrationCommand>("unapplyMigration");
        rpc_api.add_command_handler::<ResetCommand>("reset");
        rpc_api.add_command_handler::<CalculateDatamodelCommand>("calculateDatamodel");
        rpc_api.add_command_handler::<CalculateDatabaseStepsCommand>("calculateDatabaseSteps");
        rpc_api.add_command_handler::<DmmfToDmlCommand>("convertDmmfToDml");
        rpc_api.add_command_handler::<GetConfigCommand>("getConfig");
        rpc_api
    }

    fn add_command_handler<T: MigrationCommand>(&mut self, name: &str) {
        self.io_handler.add_method(name, |params: Params| {
            let input: T::Input = params.clone().parse()?;
            let engine = if T::has_source_config() {
                let source_config: SourceConfigInput = params.parse()?;
                let engine = MigrationEngine::new(&source_config.source_config, T::underlying_database_must_exist());
                engine.init();
                engine
            } else {
                // FIXME: this is ugly
                MigrationEngine::new("", T::underlying_database_must_exist())
            };
            let cmd = T::new(input);
            let result = &cmd.execute(&engine).map_err(convert_error)?;
            let response_json = serde_json::to_value(result).expect("Rendering of RPC response failed");
            Ok(response_json)
        });
    }

    pub fn handle(&self) {
        let mut json_is_complete = false;
        let mut input = String::new();
        while !json_is_complete {
            io::stdin().read_line(&mut input).expect("Reading from stdin failed.");
            json_is_complete = serde_json::from_str::<serde_json::Value>(&input).is_ok();
        }
        println!("{}", self.handle_input(&input));
    }

    pub fn handle_input(&self, input: &str) -> String {
        self.io_handler
            .handle_request_sync(&input)
            .expect("Handling the RPC request failed")
    }
}

fn convert_error(command_error: CommandError) -> jsonrpc_core::types::error::Error {
    let json = serde_json::to_value(command_error).expect("rendering the errors as json failed.");
    jsonrpc_core::types::error::Error {
        code: jsonrpc_core::types::error::ErrorCode::ServerError(4466),
        message: "An error happened. Check the data field for details.".to_string(),
        data: Some(json),
    }
}
