use database_inspector::*;
use datamodel::*;

pub struct DatabaseSchemaCalculator<'a> {
    data_model: &'a Schema,
}

impl<'a> DatabaseSchemaCalculator<'a> {
    pub fn calculate(data_model: &Schema) -> DatabaseSchema {
        let calculator = DatabaseSchemaCalculator { data_model };
        calculator.calculate_internal()
    }

    fn calculate_internal(&self) -> DatabaseSchema {
        let mut tables = Vec::new();
        let mut model_tables = self.calculate_model_tables();
        tables.append(&mut model_tables);

        DatabaseSchema { tables }
    }

    fn calculate_model_tables(&self) -> Vec<Table> {
        self.data_model
            .models()
            .iter()
            .map(|m| {
                let columns = m
                    .fields
                    .iter()
                    .flat_map(|f| match (&f.field_type, &f.arity) {
                        (FieldType::Base(scalar), arity) if arity != &FieldArity::List => Some(Column {
                            name: f.name.clone(),
                            tpe: column_type(scalar),
                            is_required: arity == &FieldArity::Required,
                            foreign_key: None,
                            sequence: None,
                        }),
                        _ => None,
                    })
                    .collect();
                Table {
                    name: m.name.clone(),
                    columns: columns,
                    indexes: Vec::new(),
                }
            })
            .collect()
    }
}

fn column_type(scalar_type: &ScalarType) -> ColumnType {
    match scalar_type {
        ScalarType::Int => ColumnType::Int,
        ScalarType::Float => ColumnType::Float,
        ScalarType::Boolean => ColumnType::Boolean,
        ScalarType::Enum => ColumnType::String,
        ScalarType::String => ColumnType::String,
        ScalarType::DateTime => ColumnType::DateTime,
        ScalarType::Decimal => unimplemented!(),
    }
}
