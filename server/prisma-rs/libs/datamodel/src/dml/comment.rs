use serde::{Deserialize, Serialize};

/// Represents a comment. Currently unused.
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub struct Comment {
    pub text: String,
    pub is_error: bool,
}
