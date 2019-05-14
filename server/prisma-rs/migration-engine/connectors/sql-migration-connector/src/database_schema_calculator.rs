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
        let mut scalar_list_tables = self.calculate_scalar_list_tables();
        tables.append(&mut model_tables);
        tables.append(&mut scalar_list_tables);

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

    fn calculate_scalar_list_tables(&self) -> Vec<Table> {
        let mut result = Vec::new();

        for model in self.data_model.models() {
            let list_fields: Vec<&Field> = model
                .fields
                .iter()
                .filter(|f| f.arity == FieldArity::List && is_scalar(f))
                .collect();
            for field in list_fields {
                let id_field = model.fields.iter().next().clone().unwrap(); // todo: find actual id field
                let table = Table {
                    name: format!("{}_{}", model.name.clone(), field.name.clone()),
                    columns: vec![
                        Column::new("nodeId".to_string(), column_type(&scalar_type(&id_field)), true),
                        Column::new("position".to_string(), ColumnType::Int, true),
                        Column::new("value".to_string(), column_type(&scalar_type(&field)), true),
                    ],
                    indexes: Vec::new(),
                };
                result.push(table);
            }
        }

        result
    }
}

fn is_scalar(field: &Field) -> bool {
    match field.field_type {
        FieldType::Base(_) => true,
        _ => false,
    }
}

fn scalar_type(field: &Field) -> &ScalarType {
    match &field.field_type {
        FieldType::Base(ref scalar) => scalar,
        x => panic!(format!(
            "only scalar types are suported here. Type is {:?} on field {}",
            x, field.name
        )),
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
