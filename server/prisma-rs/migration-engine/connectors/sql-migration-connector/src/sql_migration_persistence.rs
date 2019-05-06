use migration_connector::*;

pub struct SqlMigrationPersistence {}

#[allow(unused, dead_code)]
impl MigrationPersistence for SqlMigrationPersistence {
    fn last(&self) -> Option<Migration> {
        None
    }

    fn load_all(&self) -> Vec<Migration> {
        vec![]
    }


    fn create(&self, migration: Migration) -> Migration {
        migration
    }

    fn update(&self, migration: Migration) {        
    }
}