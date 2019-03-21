use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> Value;
}
