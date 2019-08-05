use migration_core::rpc_api::RpcApi;
use prisma_common::{logger::Logger, metrics_recorder::StupidLogRecorder};

fn main() {
    let _logger = Logger::build("migration-engine", std::io::stderr()); // keep in scope
    StupidLogRecorder::install().unwrap();

    let rpc_api = RpcApi::new();
    rpc_api.handle();
}
