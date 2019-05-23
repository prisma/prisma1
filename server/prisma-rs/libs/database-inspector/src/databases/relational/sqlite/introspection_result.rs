use crate::relational::SpecializedRelationalIntrospectionResult;

pub struct SqlLiteIntrospectionResult { }

impl SqlLiteIntrospectionResult { 
    pub fn new() -> SqlLiteIntrospectionResult {
      SqlLiteIntrospectionResult { }
    }
}

impl SpecializedRelationalIntrospectionResult for SqlLiteIntrospectionResult {
    fn database_type(&self) -> &str {
        "sqlite"
    }
}