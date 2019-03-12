
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Serialize, Deserialize)]
pub struct GraphQlBody {
    query: String,
    operation: String,
    variables: HashMap<String, String>,
}