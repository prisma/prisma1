use crate::commands::command::MigrationCommand;
use crate::commands::infer_migration_steps::InferMigrationStepsCommand;
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
        rpc_api
    }

    fn add_command_handler<T: MigrationCommand>(&mut self, name: &str) {
        self.io_handler.add_method(name, |params: Params| {
            let input: T::Input = params.parse()?;
            let cmd = T::new(input);
            let engine = MigrationEngine::new();
            let response_json = serde_json::to_value(&cmd.execute(engine)).unwrap();
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
