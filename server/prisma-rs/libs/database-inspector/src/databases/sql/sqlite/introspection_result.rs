use crate::sql::SpecializedSqlIntrospectionResult;

pub struct SqliteIntrospectionResult {}

impl SqliteIntrospectionResult {
    pub fn new() -> SqliteIntrospectionResult {
        SqliteIntrospectionResult {}
    }
}

impl SpecializedSqlIntrospectionResult for SqliteIntrospectionResult {
    fn database_type(&self) -> &str {
        "sqlite"
    }
}
