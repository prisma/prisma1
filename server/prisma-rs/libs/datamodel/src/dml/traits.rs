// Setters are a bit untypical for rust,
// but we want to have "composeable" struct creation.

/// Trait for all datamodel objects which have a name.
pub trait WithName {
    /// Gets the name.
    fn name(&self) -> &String;
    /// Sets the name.
    fn set_name(&mut self, name: &str);
}

/// Trait for all datamodel objects which have an internal database name.
pub trait WithDatabaseName {
    /// Gets the internal database name.
    fn database_name(&self) -> &Option<String>;
    /// Sets the internal database name.
    fn set_database_name(&mut self, database_name: &Option<String>);
}
