pub mod ir;
use ir::{Item, ResponseType};

/// An envelope type that encodes a particular tree reply
pub struct Envelope {
    tt: ResponseType,
    root: Item,
}
