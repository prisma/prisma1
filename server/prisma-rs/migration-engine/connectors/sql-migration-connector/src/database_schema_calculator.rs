use crate::SqlResult;
use chrono::*;
use database_introspection::*;
use datamodel::common::*;
use datamodel::*;
use prisma_models::{DatamodelConverter, TempManifestationHolder, TempRelationHolder};
use prisma_query::error::Error::ColumnNotFound;

pub struct DatabaseSchemaCalculator<'a> {
    data_model: &'a Datamodel,
}

impl<'a> DatabaseSchemaCalculator<'a> {
    pub fn calculate(data_model: &Datamodel) -> SqlResult<DatabaseSchema> {
        let calculator = DatabaseSchemaCalculator { data_model };
        calculator.calculate_internal()
    }

    fn calculate_internal(&self) -> SqlResult<DatabaseSchema> {
        let mut tables = Vec::new();
        let model_tables_without_inline_relations = self.calculate_model_tables()?;
        let mut model_tables = self.add_inline_relations_to_model_tables(model_tables_without_inline_relations)?;
        let mut scalar_list_tables = self.calculate_scalar_list_tables()?;
        let mut relation_tables = self.calculate_relation_tables()?;

        tables.append(&mut model_tables);
        tables.append(&mut scalar_list_tables);
        tables.append(&mut relation_tables);

        // guarantee same sorting as in the database-introspection
        for mut table in &mut tables {
            table.columns.sort_unstable_by_key(|col| col.name.clone());
        }

        let enums = Vec::new();
        let sequences = Vec::new();

        Ok(DatabaseSchema {
            tables,
            enums,
            sequences,
        })
    }

    fn calculate_model_tables(&self) -> SqlResult<Vec<ModelTable>> {
        self.data_model
            .models()
            .map(|model| {
                let columns = model
                    .fields()
                    .flat_map(|f| match (&f.field_type, &f.arity) {
                        (FieldType::Base(_), arity) | (FieldType::Enum(_), arity) if arity != &FieldArity::List => {
                            Some(Column {
                                name: f.db_name(),
                                tpe: column_type(f),
                                arity: column_arity(&f),
                                default: Some(f.migration_value_new(&self.data_model)),
                                auto_increment: false,
                            })
                        }
                        _ => None,
                    })
                    .collect();

                let primary_key = PrimaryKey {
                    columns: vec![model.id_field()?.db_name()],
                };
                let table = Table {
                    name: model.db_name(),
                    columns,
                    indices: Vec::new(),
                    primary_key: Some(primary_key),
                    foreign_keys: Vec::new(),
                };
                Ok(ModelTable {
                    model: model.clone(),
                    table,
                })
            })
            .collect()
    }

    fn calculate_scalar_list_tables(&self) -> SqlResult<Vec<Table>> {
        let mut result = Vec::new();

        for model in self.data_model.models() {
            let list_fields: Vec<&Field> = model
                .fields()
                .filter(|f| f.arity == FieldArity::List && is_scalar(f))
                .collect();
            for field in list_fields {
                let id_field = model.id_field()?;
                let primary_key = PrimaryKey {
                    columns: vec!["nodeId".to_string(), "position".to_string()],
                };
                let foreign_keys = vec![ForeignKey {
                    columns: vec!["nodeId".to_string()],
                    referenced_table: model.db_name(),
                    referenced_columns: vec![model.id_field()?.db_name()],
                    on_delete_action: ForeignKeyAction::Cascade,
                }];
                let table = Table {
                    name: format!("{}_{}", model.db_name(), field.db_name()),
                    columns: vec![
                        Column {
                            name: "nodeId".to_string(),
                            tpe: column_type(&id_field),
                            arity: ColumnArity::Required,
                            default: None,
                            auto_increment: false,
                        },
                        Column {
                            name: "position".to_string(),
                            tpe: ColumnType::pure(ColumnTypeFamily::Int),
                            arity: ColumnArity::Required,
                            default: None,
                            auto_increment: false,
                        },
                        Column {
                            name: "value".to_string(),
                            tpe: column_type(&field),
                            arity: ColumnArity::Required,
                            default: None,
                            auto_increment: false,
                        },
                    ],
                    indices: Vec::new(),
                    primary_key: Some(primary_key),
                    foreign_keys,
                };
                result.push(table);
            }
        }

        Ok(result)
    }

    fn add_inline_relations_to_model_tables(&self, model_tables: Vec<ModelTable>) -> SqlResult<Vec<Table>> {
        let mut result = Vec::new();
        let relations = self.calculate_relations();
        for mut model_table in model_tables {
            for relation in relations.iter() {
                match &relation.manifestation {
                    TempManifestationHolder::Inline {
                        in_table_of_model,
                        column,
                    } if in_table_of_model == &model_table.model.name => {
                        let (model, related_model) = if model_table.model == relation.model_a {
                            (&relation.model_a, &relation.model_b)
                        } else {
                            (&relation.model_b, &relation.model_a)
                        };
                        let field = model.fields().find(|f| &f.db_name() == column).unwrap();
                        let foreign_key = ForeignKey {
                            columns: vec![column.to_string()],
                            referenced_table: related_model.db_name(),
                            referenced_columns: vec![related_model.id_field()?.db_name()],
                            on_delete_action: ForeignKeyAction::SetNull,
                        };
                        let column = Column {
                            name: column.to_string(),
                            tpe: column_type(related_model.id_field()?),
                            arity: column_arity(&field),
                            default: None,
                            auto_increment: false,
                        };
                        model_table.table.columns.push(column);
                        model_table.table.foreign_keys.push(foreign_key)
                    }
                    _ => {}
                }
            }
            result.push(model_table.table);
        }
        Ok(result)
    }

    fn calculate_relation_tables(&self) -> SqlResult<Vec<Table>> {
        let mut result = Vec::new();
        for relation in self.calculate_relations().iter() {
            match &relation.manifestation {
                TempManifestationHolder::Table => {
                    let foreign_keys = vec![
                        ForeignKey {
                            columns: vec![relation.model_a_column()],
                            referenced_table: relation.model_a.db_name(),
                            referenced_columns: vec![relation.model_a.id_field()?.db_name()],
                            on_delete_action: ForeignKeyAction::Cascade,
                        },
                        ForeignKey {
                            columns: vec![relation.model_b_column()],
                            referenced_table: relation.model_b.db_name(),
                            referenced_columns: vec![relation.model_b.id_field()?.db_name()],
                            on_delete_action: ForeignKeyAction::Cascade,
                        },
                    ];
                    let table = Table {
                        name: relation.table_name(),
                        columns: vec![
                            Column {
                                name: relation.model_a_column(),
                                tpe: column_type(relation.model_a.id_field()?),
                                arity: ColumnArity::Required,
                                default: None,
                                auto_increment: false,
                            },
                            Column {
                                name: relation.model_b_column(),
                                tpe: column_type(relation.model_b.id_field()?),
                                arity: ColumnArity::Required,
                                default: None,
                                auto_increment: false,
                            },
                        ],
                        indices: vec![Index {
                            name: format!("{}_AB_unique", relation.table_name()),
                            columns: vec![relation.model_a_column(), relation.model_b_column()],
                            unique: true,
                        }],
                        primary_key: None,
                        foreign_keys,
                    };
                    result.push(table);
                }
                _ => {}
            }
        }
        Ok(result)
    }

    fn calculate_relations(&self) -> Vec<TempRelationHolder> {
        DatamodelConverter::calculate_relations(&self.data_model)
    }
}

#[derive(PartialEq, Debug)]
struct ModelTable {
    table: Table,
    model: Model,
}

pub trait ModelExtensions {
    fn id_field(&self) -> Result<&Field, String>;

    fn db_name(&self) -> String;
}

impl ModelExtensions for Model {
    fn id_field(&self) -> Result<&Field, String> {
        match self.fields().find(|f| f.is_id()) {
            Some(f) => Ok(f),
            None => Err(format!("Model {} does not have an id field", self.name)),
        }
    }

    fn db_name(&self) -> String {
        self.database_name.clone().unwrap_or_else(|| self.name.clone())
    }
}

pub trait FieldExtensions {
    fn is_id(&self) -> bool;

    fn is_list(&self) -> bool;

    fn is_required(&self) -> bool;

    fn db_name(&self) -> String;

    fn migration_value(&self, datamodel: &Datamodel) -> Value;

    fn migration_value_new(&self, datamodel: &Datamodel) -> String;
}

impl FieldExtensions for Field {
    fn is_id(&self) -> bool {
        self.id_info.is_some()
    }

    fn is_list(&self) -> bool {
        self.arity == FieldArity::List
    }

    fn is_required(&self) -> bool {
        self.arity == FieldArity::Required
    }

    fn db_name(&self) -> String {
        self.database_name.clone().unwrap_or_else(|| self.name.clone())
    }

    fn migration_value(&self, datamodel: &Datamodel) -> Value {
        self.default_value
            .clone()
            .unwrap_or_else(|| default_migration_value(&self.field_type, datamodel))
    }

    fn migration_value_new(&self, datamodel: &Datamodel) -> String {
        let value = match &self.default_value {
            Some(x) => match x {
                PrismaValue::Expression(_, _, _) => default_migration_value(&self.field_type, datamodel),
                x => x.clone(),
            },
            None => default_migration_value(&self.field_type, datamodel),
        };
        match value {
            Value::Boolean(x) => {
                if x {
                    "true".to_string()
                } else {
                    "false".to_string()
                }
            }
            Value::Int(x) => format!("{}", x),
            Value::Float(x) => format!("{}", x),
            Value::Decimal(x) => format!("{}", x),
            Value::String(x) => format!("{}", x),

            Value::DateTime(x) => {
                let mut raw = format!("{}", x); // this will produce a String 1970-01-01 00:00:00 UTC
                raw.truncate(raw.len() - 4); // strip the UTC suffix
                format!("{}", raw)
            }
            Value::ConstantLiteral(x) => format!("{}", x), // this represents enum values
            Value::Expression(_, _, _) => {
                unreachable!("expressions must have been filtered out in the preceding pattern match")
            }
        }
    }
}

fn default_migration_value(field_type: &FieldType, datamodel: &Datamodel) -> Value {
    match field_type {
        FieldType::Base(PrismaType::Boolean) => Value::Boolean(false),
        FieldType::Base(PrismaType::Int) => Value::Int(0),
        FieldType::Base(PrismaType::Float) => Value::Float(0.0),
        FieldType::Base(PrismaType::String) => Value::String("".to_string()),
        FieldType::Base(PrismaType::Decimal) => Value::Decimal(0.0),
        FieldType::Base(PrismaType::DateTime) => {
            let naive = NaiveDateTime::from_timestamp(0, 0);
            let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);
            PrismaValue::DateTime(datetime)
        }
        FieldType::Enum(ref enum_name) => {
            let inum = datamodel
                .find_enum(&enum_name)
                .expect(&format!("Enum {} was not present in the Datamodel.", enum_name));
            let first_value = inum
                .values
                .first()
                .expect(&format!("Enum {} did not contain any values.", enum_name));
            Value::String(first_value.to_string())
        }
        _ => unimplemented!("this functions must only be called for scalar fields"),
    }
}

fn is_scalar(field: &Field) -> bool {
    match field.field_type {
        FieldType::Base(_) => true,
        FieldType::Enum(_) => true,
        _ => false,
    }
}

fn column_type(field: &Field) -> ColumnType {
    match &field.field_type {
        FieldType::Base(ref scalar) => column_type_for_scalar_type(&scalar),
        FieldType::Enum(_) => column_type_for_scalar_type(&ScalarType::String),
        x => panic!(format!(
            "This field type is not suported here. Field type is {:?} on field {}",
            x, field.name
        )),
    }
}

fn column_arity(field: &Field) -> ColumnArity {
    match &field.arity {
        FieldArity::Required => ColumnArity::Required,
        FieldArity::List => ColumnArity::List,
        FieldArity::Optional => ColumnArity::Nullable,
    }
}

fn column_type_for_scalar_type(scalar_type: &ScalarType) -> ColumnType {
    match scalar_type {
        ScalarType::Int => ColumnType::pure(ColumnTypeFamily::Int),
        ScalarType::Float => ColumnType::pure(ColumnTypeFamily::Float),
        ScalarType::Boolean => ColumnType::pure(ColumnTypeFamily::Boolean),
        ScalarType::String => ColumnType::pure(ColumnTypeFamily::String),
        ScalarType::DateTime => ColumnType::pure(ColumnTypeFamily::DateTime),
        ScalarType::Decimal => unimplemented!(),
    }
}
