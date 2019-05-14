use serde::{Serialize, Deserialize};

#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Comment {
    pub text: String,
    pub is_error: bool,
}
