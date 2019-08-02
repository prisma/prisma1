use database_introspection::IntrospectionConnector;

pub struct IntrospectionImpl {
    pub inner: Box<IntrospectionConnector>
}

impl super::DatabaseInspector for IntrospectionImpl {
    fn introspect(&self, schema: &String) -> super::DatabaseSchema {
        let db_schema = self.inner.introspect(schema).unwrap();
        convert(db_schema)
    }
}

fn convert(db_schema: database_introspection::DatabaseSchema) -> super::DatabaseSchema {
    super::DatabaseSchema {
        tables: db_schema.tables.iter().map(convert_table).collect(),
    }
}

fn convert_table(table: &database_introspection::Table) -> super::Table {
    super::Table{
        name: table.name.clone(),
        columns: table.columns.iter().map(|c| convert_column(table, c)).collect(),
        indexes: Vec::new(),
        primary_key_columns: match &table.primary_key {
            Some(pk) => pk.columns.clone(),
            None => Vec::new(),
        },
    }
}

fn convert_column(table: &database_introspection::Table, column: &database_introspection::Column) -> super::Column {
    let fk = table.foreign_keys.iter().find(|fk|{
       fk.columns == vec![column.name.clone()]
    }).map(|fk|{
        super::ForeignKey {
            name: None,
            table: fk.referenced_table.clone(),
            column: fk.referenced_columns.clone().pop().unwrap(),
        }
    });
    super::Column {
        name: column.name.clone(),
        default: None,  // TODO: fix this
        foreign_key: fk,
        is_required: column.arity == database_introspection::ColumnArity::Required,
        sequence: None,
        tpe: convert_column_type(&column.tpe),
    }
}

fn convert_column_type(column: &database_introspection::ColumnType) -> super::ColumnType {
    match column.family {
        database_introspection::ColumnTypeFamily::Int => super::ColumnType::Int,
        database_introspection::ColumnTypeFamily::Float => super::ColumnType::Float,
        database_introspection::ColumnTypeFamily::Boolean => super::ColumnType::Boolean,
        database_introspection::ColumnTypeFamily::String => super::ColumnType::String,
        database_introspection::ColumnTypeFamily::DateTime => super::ColumnType::DateTime,
        database_introspection::ColumnTypeFamily::Binary => unreachable!(),
        database_introspection::ColumnTypeFamily::Json => unreachable!(),
        database_introspection::ColumnTypeFamily::Uuid => unreachable!(),
    }
}