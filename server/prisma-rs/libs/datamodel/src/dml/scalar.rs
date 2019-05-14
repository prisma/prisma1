use chrono::{DateTime, Utc};
use serde::{Serialize, Deserialize};

#[derive(Debug, Copy, PartialEq, Clone, Serialize, Deserialize)]
pub enum ScalarType {
    Int,
    Float,
    Decimal,
    Boolean,
    String,
    DateTime,
    Enum,
}

// TODO, Check if data types are correct
#[derive(Debug, PartialEq, Clone, Serialize, Deserialize)]
pub enum Value {
    Int(i32),
    Float(f32),
    Decimal(f32),
    Boolean(bool),
    String(String),
    DateTime(DateTime<Utc>),
    ConstantLiteral(String),
}
