use datamodel::*;
use migration_connector::steps::*;

pub trait DataModelCalculator {
    fn infer(&self, current: &Schema, steps: Vec<MigrationStep>) -> Schema {
        DataModelCalculatorImpl { current, steps }.infer()
    }
}

pub struct DataModelCalculatorSingleton {}
impl DataModelCalculator for DataModelCalculatorSingleton {}

struct DataModelCalculatorImpl<'a> {
    current: &'a Schema,
    steps: Vec<MigrationStep>,
}

impl<'a> DataModelCalculatorImpl<'a> {
    fn infer(&self) -> Schema {
        Schema::empty()
    }
}
