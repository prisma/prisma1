use serde::de::DeserializeOwned;
use serde::Serialize;

pub trait MigrationCommand {
    type Input: DeserializeOwned;
    type Output: Serialize;

    fn new(input: Self::Input) -> Box<Self>;
    fn execute(&self) -> Self::Output;
}
