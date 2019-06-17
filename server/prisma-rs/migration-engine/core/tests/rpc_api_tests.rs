#![allow(non_snake_case)]
mod test_harness;
use migration_core::rpc_api::RpcApi;
use test_harness::*;

#[test]
fn simple_end_to_end_test() {
    let json = format!(
        r#"
        {{
            "id": 1,
            "jsonrpc": "2.0",
            "method": "listMigrations",
            "params": {{
                "sourceConfig": {}
            }}
        }}
    "#,
        test_config_json_escaped()
    );

    let result = handle_command(&json);
    assert_eq!(result, r#"{"jsonrpc":"2.0","result":[],"id":1}"#);
}

#[test]
fn error_if_the_datamodel_is_invalid() {
    let json = format!(
        r#"
        {{
            "id": 1,
            "jsonrpc": "2.0",
            "method": "inferMigrationSteps",
            "params": {{
                "sourceConfig": {},
                "migrationId": "the-migration_id",
                "assumeToBeApplied": [],
                "dataModel": "model Blog {{ id Int @id @default(cuid()) }}"
            }}
        }}
    "#,
        test_config_json_escaped()
    );

    let result = handle_command(&json);
    // let result_json = serde_json::from_str::<serde_json::Value>(&result).unwrap();
    // let expected = serde_json::from_str::<serde_json::Value>(&r#"{"jsonrpc":"2.0","error":{"code":4466,"message":"An error happened. Check the data field for details.","data":{"code":1000,"errors":["Error parsing attribute @default: Expected Int, but received String value Function<String>"],"type":"DataModelErrors"}},"id":1}"#).unwrap();
    // assert_eq!(result_json, expected);
    dbg!(&result);
    assert!(
        result.contains("Error parsing attribute \\\"@default\\\": Expected a Int value, but received String value \\\"Function<String>\\\".")
    );
}

fn handle_command(command: &str) -> String {
    // just using the engine to reset the db
    let engine = test_engine(&sqlite_test_config());
    engine.reset().unwrap();
    let rpc_api = RpcApi::new();
    rpc_api.handle_input(command)
}

pub fn test_config_json_escaped() -> String {
    let config = sqlite_test_config();
    serde_json::to_string(&serde_json::Value::String(config)).unwrap()
}
