
use chrono::{DateTime, Utc};

#[derive(Debug, Copy, PartialEq, Clone)]
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
#[derive(Debug, PartialEq, Clone)]
pub enum Value {
    Int(i32),
    Float(f32),
    Decimal(f32),
    Boolean(bool),
    String(String),
    DateTime(DateTime<Utc>),
    ConstantLiteral(String),
}