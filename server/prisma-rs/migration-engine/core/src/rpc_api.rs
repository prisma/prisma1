use crate::commands::*;
use crate::migration_engine::*;
use jsonrpc_core;
use jsonrpc_core::IoHandler;
use jsonrpc_core::*;
use sql_migration_connector::SqlMigrationConnector;
use std::io;
// TODO: At one point, we will replace the top level implementation by
// something also suited for document.
use prisma_query::{connector::Sqlite as SqliteDatabaseClient, transaction::Connectional};

pub struct RpcApi {
    io_handler: jsonrpc_core::IoHandler<()>,
    client: std::sync::Arc<SqliteDatabaseClient>,
}

impl RpcApi {
    pub fn new() -> RpcApi {
        // TODO: Inject database connection information!
        let server_root = std::env::var("SERVER_ROOT").expect("Env var SERVER_ROOT required but not found.");
        let path = format!("{}/db", server_root);

        let mut rpc_api = RpcApi {
            io_handler: IoHandler::new(),
            // TODO: Refactor out client generation
            client: std::sync::Arc::new(SqliteDatabaseClient::new(path, 32, false).unwrap()),
        };
        rpc_api.add_command_handler::<InferMigrationStepsCommand>("inferMigrationSteps");
        rpc_api.add_command_handler::<ListMigrationStepsCommand>("listMigrations");
        rpc_api.add_command_handler::<MigrationProgressCommand>("migrationProgress");
        rpc_api.add_command_handler::<ApplyMigrationCommand>("applyMigration");
        rpc_api.add_command_handler::<UnapplyMigrationCommand>("unapplyMigration");
        rpc_api
    }

    fn add_command_handler<T: MigrationCommand>(&mut self, name: &str) {
        let client = self.client.clone(); // Explicitely copy arc.

        self.io_handler.add_method(name, move |params: Params| {
            let input: T::Input = params.parse()?;
            let cmd = T::new(input);

            let schema_name = "?";

            // Move out connection creation.
            let response_json = client
                .with_shared_connection(schema_name, |connection| {
                    // TODO: At one point, we will need to distinguish the database type.
                    let connector = SqlMigrationConnector::new(schema_name, connection);

                    let engine = MigrationEngine::new(&connector, schema_name);

                    let response_json = serde_json::to_value(&cmd.execute(&engine)).unwrap();
                    Ok(response_json)

                    // TODO: Sane Error Type
                })
                .unwrap();

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
