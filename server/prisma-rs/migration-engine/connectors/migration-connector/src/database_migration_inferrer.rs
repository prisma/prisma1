use crate::MigrationStep;
use datamodel::Datamodel;

pub trait DatabaseMigrationInferrer<T> {
    fn infer(&self, previous: &Datamodel, next: &Datamodel, steps: &Vec<MigrationStep>) -> T;
}
