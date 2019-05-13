

// Setters are a bit untypical for rust,
// but we want to have "composeable" struct creation.
pub trait WithName {
    fn name(&self) -> &String;
    fn set_name(&mut self, name: &String);
}

pub trait WithDatabaseName {
    fn database_name(&self) -> &Option<String>;
    fn set_database_name(&mut self, database_name: &Option<String>);
}
