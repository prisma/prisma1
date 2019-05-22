use crate::relational::SpecializedRelationalIntrospectionResult;

pub struct SqlLiteIntrospectionResult { }

impl SpecializedRelationalIntrospectionResult for SqlLiteIntrospectionResult {
    fn database_type(&self) -> &str {
        "sqllite"
    }
}