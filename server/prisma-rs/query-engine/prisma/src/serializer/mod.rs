pub mod ir;
use ir::{Item, ResponseType};
use serde::Serialize;

/// An envelope type that encodes a particular tree reply
#[derive(Serialize)]
pub struct Envelope {
    tt: ResponseType,
    root: Item,
}

impl Envelope {
    pub fn convert(self) -> serde_json::Value {
        serde_json::to_value(&self).unwrap()
    }
}
