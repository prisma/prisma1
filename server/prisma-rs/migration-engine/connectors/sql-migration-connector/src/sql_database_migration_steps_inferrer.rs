use crate::sql_migration_step::*;
use datamodel::*;
use itertools::{Either, Itertools};
use migration_connector::steps::*;
use migration_connector::*;
use std::collections::HashMap;

pub struct SqlDatabaseMigrationStepsInferrer {}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepsInferrer<SqlMigrationStep> for SqlDatabaseMigrationStepsInferrer {
    fn infer(&self, previous: &Schema, next: &Schema, steps: Vec<MigrationStep>) -> Vec<SqlMigrationStep> {
        let creates: Vec<CreateModelOrField> = steps
            .into_iter()
            .flat_map(|step| match step {
                MigrationStep::CreateModel(x) => Some(CreateModelOrField::Model(x)),
                MigrationStep::CreateField(x) => Some(CreateModelOrField::Field(x)),
                _ => None,
            })
            .collect();
        let (create_models, create_fields): (Vec<CreateModel>, Vec<CreateField>) =
            creates.into_iter().partition_map(|step| match step {
                CreateModelOrField::Model(x) => Either::Left(x),
                CreateModelOrField::Field(x) => Either::Right(x),
            });
        let mut create_fields_map: HashMap<String, Vec<CreateField>> = HashMap::new();
        for (model_name, create_fieldses) in &create_fields.into_iter().group_by(|cf| cf.model.clone()) {
            create_fields_map.insert(model_name, create_fieldses.into_iter().collect());
        }

        let mut grouped_steps: HashMap<CreateModel, Vec<CreateField>> = HashMap::new();

        for cm in create_models {
            let cfs = create_fields_map.remove(&cm.name).unwrap_or(Vec::new());
            grouped_steps.insert(cm, cfs);
        }

        let mut create_tables: Vec<CreateTable> = Vec::new();
        for (create_model, create_fields) in grouped_steps {
            let columns = create_fields
                .into_iter()
                .map(|cf| ColumnDescription {
                    name: cf.name,
                    tpe: column_type(cf.tpe),
                    required: cf.arity == FieldArity::Required,
                })
                .collect();
            let create_table = CreateTable {
                name: create_model.name,
                columns: columns,
            };
            create_tables.push(create_table);
        }

        let mut sql_steps = Vec::new();
        sql_steps.append(&mut wrap_as_step(create_tables, |x| SqlMigrationStep::CreateTable(x)));
        sql_steps
    }
}

fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}

fn column_type(ft: FieldType) -> Box<ColumnType> {
    match ft {
        FieldType::Base(scalar) => Box::new(scalar),
        _ => panic!("Only scalar types are supported here"),
    }
}

impl ColumnType for ScalarType {
    fn render(&self) -> String {
        match self {
            ScalarType::Int => "Int".to_string(),
            ScalarType::Float => "Float".to_string(),
            _ => unimplemented!(),
        }
    }
}

enum CreateModelOrField {
    Model(CreateModel),
    Field(CreateField),
}
