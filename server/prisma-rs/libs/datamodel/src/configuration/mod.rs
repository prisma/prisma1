mod source;
pub use source::*;
mod generator;
pub use generator::*;
mod json;
pub use json::*;

pub struct Configuration {
    pub generators: Vec<Generator>,
    pub datasources: Vec<Box<Source>>,
}
