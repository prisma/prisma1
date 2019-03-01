mod commands;
mod migration;
mod rpc_api;
mod steps;

use rpc_api::RpcApi;
use serde::de::DeserializeOwned;
use serde::Serialize;
use steps::*;

#[macro_use]
extern crate serde_derive;

fn main() {
//    test_json_serialization::<MigrationStep>(r#"{"name":"Blog","stepType":"CreateModel"}"#);
//    test_json_serialization::<MigrationStep>(r#"{"name":"Blog","stepType":"UpdateModel"}"#);

    let rpc_api = RpcApi::new();
    rpc_api.handle();
}

fn test_json_serialization<T: DeserializeOwned + Serialize + std::fmt::Debug>(json: &str) {
    println!("input       : {}", json);
    let deserialized: T = serde_json::from_str(&json).unwrap();
    println!("deserialized: {:?}", deserialized);
    let serialized_again = serde_json::to_string(&deserialized).unwrap();
    println!("serialized  : {}", serialized_again);
}
