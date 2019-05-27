use database_inspector::sql::*;

pub trait DatabaseSchemaAsserts {
    fn assert_has_table(&self, t: &str) -> &TableInfo;
    fn assert_has_relation(&self, source_table: &str, source_column: &str, target_table: &str, target_column: &str) -> &TableRelationInfo;
}

pub trait TableAsserts {
    fn assert_has_column(&self, t: &str) -> &ColumnInfo;
}

pub trait ColumnAsserts {
    fn assert_is_unique(&self, b: bool) -> &ColumnInfo;
    fn assert_default_value(&self, v: Option<String>) -> &ColumnInfo;
    fn assert_column_type(&self, t: ColumnType) -> &ColumnInfo;
    fn assert_is_nullable(&self, b: bool) -> &ColumnInfo;
    fn assert_is_list(&self, b: bool) -> &ColumnInfo;
    fn assert_is_auto_increment(&self, b: bool) -> &ColumnInfo;
    fn assert_is_primary_key(&self, b: bool) -> &ColumnInfo;
}

impl DatabaseSchemaAsserts for DatabaseSchemaInfo {
    fn assert_has_table(&self, t: &str) -> &TableInfo {
        self.tables.iter().find(|m| &m.name == t)
            .expect(&format!("Table not found {}", t))
    }

    fn assert_has_relation(&self, source_table: &str, source_column: &str, target_table: &str, target_column: &str) -> &TableRelationInfo {
        self.relations.iter().find(|t| &t.source_table == source_table && &t.source_column == source_column && &t.target_table == target_table &&t.target_column == target_column)
            .expect(&format!("Relation not found {}.{} -> {}.{}", source_table, source_column, target_table, target_column))
    }
}

impl TableAsserts for TableInfo {
    fn assert_has_column(&self, t: &str) -> &ColumnInfo {
        self.columns.iter().find(|m| &m.name == t)
            .expect(&format!("Column not found {}", t))
    }
}

impl ColumnAsserts for ColumnInfo {
    fn assert_is_unique(&self, b: bool) -> &ColumnInfo {
        assert_eq!(self.is_unique, b);
        self
    }
    fn assert_default_value(&self, v: Option<String>) -> &ColumnInfo {
        assert_eq!(self.default_value, v);
        self
    }
    fn assert_column_type(&self, t: ColumnType) -> &ColumnInfo {
        assert_eq!(self.column_type, t);
        self
    }
    fn assert_is_nullable(&self, b: bool) -> &ColumnInfo {
        assert_eq!(self.is_nullable, b);
        self
    }
    fn assert_is_list(&self, b: bool) -> &ColumnInfo {
        assert_eq!(self.is_list, b);
        self
    }
    fn assert_is_auto_increment(&self, b: bool) -> &ColumnInfo {
        assert_eq!(self.is_auto_increment, b);
        self
    }
    fn assert_is_primary_key(&self, b: bool) -> &ColumnInfo {
        assert_eq!(self.is_primary_key, b);
        self
    }
}