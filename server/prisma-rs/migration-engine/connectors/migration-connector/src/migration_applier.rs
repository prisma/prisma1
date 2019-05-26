use crate::*;

pub trait MigrationApplier<T> {
    type ErrorType;
    type ConnectionType;

    fn apply_steps(&mut self, connection: Self::ConnectionType, migration: Migration, steps: Vec<T>) -> Result<(), Self::ErrorType>;
}