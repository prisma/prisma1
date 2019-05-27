#[allow(unused_imports)]
use migration_core::rpc_api::RpcApi;

fn main() {
    let rpc_api = RpcApi::new();
    rpc_api.handle();
}
