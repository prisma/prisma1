use crate::relational::SpecializedRelationalIntrospectionConnector;
use prisma_query::ast::*;

pub struct SqlLiteConnector { }

fn result_list_to_string_vec(res: &ResultSet) -> Result<Vec<String>, SqlError> {
    let mut names: Vec<String> = vec![];

    for row in res {
        names.push(String::from(row.as_str(0)?))
    }

    Ok(names)
}

impl SpecializedRelationalIntrospectionConnector for SqlLiteConnector {
    fn database_type(&self) -> &str {
        "sqllite"
    }
    fn list_schemas(&self, transaction: &mut Transaction) -> Result<Vec<String>, SqlError> {}
        // RAW:
        //   SELECT name FROM {}.sqlite_master WHERE type='table'
        let query = Select::from("{}.databases")
            .column("name")

        let res = transaction.query(Query::from(query))?;

        result_list_to_string_vec(res)?
    }

    fn query_tables(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<String>, SqlError> {
        let query = Select::from("{}.sqlite_master")
            .column("name")
            .so_that(Column::from("type").equal("table"))

        let res = transaction.query(Query::from(query))?;

        result_list_to_string_vec(res)?
    }
    
    fn query_relations(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError> ;
    fn query_columns(&self, transaction: &mut Transaction, schema: &str, table: &str) -> Result<Vec<ColumnInfo>, SqlError> ;
    fn query_column_comment(&self, transaction: &mut Transaction, schema: &str, table: &str, column: &str) -> Result<Option<String>, SqlError> ;
    fn query_indices(&self, transaction: &mut Transaction, schema: &str, table: &str) -> Result<Vec<InternalIndexIntrospectionResult>, SqlError> ;
    fn query_enums(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<EnumInfo>, SqlError> ;
    fn query_sequences(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<SequenceInfo>, SqlError> ;
    fn create_introspection_result(&self, models: &Vec<TableInfo>, relations: &Vec<TableRelationInfo>, enums: &Vec<EnumInfo>, sequences: &Vec<SequenceInfo>) -> RelationalIntrospectionResult;

}