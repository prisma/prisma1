use database_inspector::*;
use datamodel::*;
use itertools::Itertools;

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
        let model_tables_without_inline_relations = self.calculate_model_tables();
        let mut model_tables = self.add_inline_relations_to_model_tables(model_tables_without_inline_relations);
        let mut scalar_list_tables = self.calculate_scalar_list_tables();
        let mut relation_tables = self.calculate_relation_tables();

        tables.append(&mut model_tables);
        tables.append(&mut scalar_list_tables);
        tables.append(&mut relation_tables);

        DatabaseSchema { tables }
    }

    fn calculate_model_tables(&self) -> Vec<ModelTable> {
        self.data_model
            .models()
            .map(|model| {
                let columns = model
                    .fields()
                    .flat_map(|f| match (&f.field_type, &f.arity) {
                        (FieldType::Base(_), arity) if arity != &FieldArity::List => Some(Column {
                            name: f.db_name(),
                            tpe: column_type(f),
                            is_required: arity == &FieldArity::Required,
                            foreign_key: None,
                            sequence: None,
                        }),
                        _ => None,
                    })
                    .collect();

                let table = Table {
                    name: model.db_name(),
                    columns: columns,
                    indexes: Vec::new(),
                    primary_key_columns: vec![model.id_field().db_name()],
                };
                ModelTable {
                    model: model.clone(),
                    table: table,
                }
            })
            .collect()
    }

    fn calculate_scalar_list_tables(&self) -> Vec<Table> {
        let mut result = Vec::new();

        for model in self.data_model.models() {
            let list_fields: Vec<&Field> = model
                .fields()
                .filter(|f| f.arity == FieldArity::List && is_scalar(f))
                .collect();
            for field in list_fields {
                let id_field = model.id_field();
                let table = Table {
                    name: format!("{}_{}", model.db_name(), field.db_name()),
                    columns: vec![
                        Column::with_foreign_key(
                            "nodeId".to_string(),
                            column_type(&id_field),
                            true,
                            ForeignKey {
                                table: model.db_name(),
                                column: model.id_field().db_name(),
                            },
                        ),
                        Column::new("position".to_string(), ColumnType::Int, true),
                        Column::new("value".to_string(), column_type(&field), true),
                    ],
                    indexes: Vec::new(),
                    primary_key_columns: vec!["nodeId".to_string(), "position".to_string()],
                };
                result.push(table);
            }
        }

        result
    }

    fn add_inline_relations_to_model_tables(&self, model_tables: Vec<ModelTable>) -> Vec<Table> {
        let mut result = Vec::new();
        let relations = self.calculate_relations();
        for mut model_table in model_tables {
            for relation in relations.iter() {
                match &relation.manifestation {
                    RelationManifestation::Inline {
                        in_table_of_model,
                        column,
                    } if in_table_of_model == &model_table.model.name => {
                        let (field, related_model) = if model_table.model == relation.model_a {
                            (&relation.field_a, &relation.model_b)
                        } else {
                            (&relation.field_b, &relation.model_a)
                        };
                        let column = Column::with_foreign_key(
                            column.to_string(),
                            column_type(&model_table.model.id_field()),
                            field.is_required(),
                            ForeignKey {
                                table: related_model.db_name(),
                                column: related_model.id_field().db_name(),
                            },
                        );
                        model_table.table.columns.push(column);
                    }
                    _ => {}
                }
            }
            result.push(model_table.table);
        }
        result
    }

    fn calculate_relation_tables(&self) -> Vec<Table> {
        let mut result = Vec::new();
        for relation in self.calculate_relations().iter() {
            match &relation.manifestation {
                RelationManifestation::Table {
                    model_a_column,
                    model_b_column,
                } => {
                    let table = Table {
                        name: relation.table_name(),
                        columns: vec![
                            Column::with_foreign_key(
                                model_a_column.to_string(),
                                column_type(&relation.model_a.id_field()),
                                true,
                                ForeignKey {
                                    table: relation.model_a.db_name(),
                                    column: relation.model_a.id_field().db_name(),
                                },
                            ),
                            Column::with_foreign_key(
                                model_b_column.to_string(),
                                column_type(&relation.model_b.id_field()),
                                true,
                                ForeignKey {
                                    table: relation.model_b.db_name(),
                                    column: relation.model_b.id_field().db_name(),
                                },
                            ),
                        ],
                        indexes: Vec::new(),
                        primary_key_columns: Vec::new(),
                    };
                    result.push(table);
                }
                _ => {}
            }
        }
        result
    }

    #[allow(unused)]
    fn calculate_relations(&self) -> Vec<Relation> {
        let mut result = Vec::new();
        for model in self.data_model.models() {
            for field in model.fields() {
                match &field.field_type {
                    FieldType::Relation(relation_info) => {
                        let RelationInfo {
                            to,
                            to_field: _,
                            name: _,
                            on_delete: _,
                        } = relation_info;
                        let related_model = self.data_model.find_model(&to).unwrap();
                        // TODO: handle case of implicit back relation field
                        let related_field = related_model
                            .fields()
                            .find(|f| related_type(f) == Some(model.name.to_string()))
                            .unwrap()
                            .clone();

                        let (model_a, model_b, field_a, field_b) = match () {
                            _ if &model.name < &related_model.name => {
                                (model.clone(), related_model.clone(), field.clone(), related_field)
                            }
                            _ if &related_model.name < &model.name => {
                                (related_model.clone(), model.clone(), related_field, field.clone())
                            }
                            _ => (model.clone(), related_model.clone(), field.clone(), related_field),
                        };
                        let manifestation = match (field_a.is_list(), field_b.is_list()) {
                            (true, true) => RelationManifestation::Table {
                                model_a_column: "A".to_string(),
                                model_b_column: "B".to_string(),
                            },
                            (false, true) => RelationManifestation::Inline {
                                in_table_of_model: model_a.name.clone(),
                                column: field_a.db_name(),
                            },
                            (true, false) => RelationManifestation::Inline {
                                in_table_of_model: model_b.name.clone(),
                                column: field_b.db_name(),
                            },
                            (false, false) => unimplemented!(), // 1 to 1. choose semi randomly
                        };

                        result.push(Relation {
                            model_a: model_a,
                            model_b: model_b,
                            field_a: field_a,
                            field_b: field_b,
                            manifestation,
                        })
                    }
                    _ => {}
                }
            }
        }
        result.into_iter().unique_by(|rel| rel.name()).collect()
    }
}

#[derive(PartialEq, Debug)]
struct ModelTable {
    table: Table,
    model: Model,
}

#[derive(PartialEq, Debug, Clone)]
struct Relation {
    model_a: Model,
    model_b: Model,
    field_a: Field,
    field_b: Field,
    manifestation: RelationManifestation,
}

impl Relation {
    fn name(&self) -> String {
        // TODO: must replicate behaviour of `generateRelationName` from `SchemaInferrer`
        format!("{}To{}", &self.model_a.name, &self.model_b.name)
    }

    fn table_name(&self) -> String {
        format!("_{}", self.name())
    }

    #[allow(unused)]
    fn is_many_to_many(&self) -> bool {
        self.field_a.is_list() && self.field_b.is_list()
    }
}

#[derive(PartialEq, Debug, Clone)]
enum RelationManifestation {
    Inline {
        in_table_of_model: String,
        column: String,
    },
    Table {
        model_a_column: String,
        model_b_column: String,
    },
}

trait ModelExtensions {
    fn id_field(&self) -> &Field;

    fn db_name(&self) -> String;
}

impl ModelExtensions for Model {
    // todo: find actual id field
    fn id_field(&self) -> &Field {
        self.fields().find(|f| f.is_id()).unwrap()
    }

    fn db_name(&self) -> String {
        self.database_name.clone().unwrap_or_else(|| self.name.clone())
    }
}

trait FieldExtensions {
    fn is_id(&self) -> bool;

    fn is_list(&self) -> bool;

    fn is_required(&self) -> bool;

    fn db_name(&self) -> String;
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
}

fn related_type(field: &Field) -> Option<String> {
    match &field.field_type {
        FieldType::Relation(relation_info) => Some(relation_info.to.to_string()),
        _ => None,
    }
}

fn is_scalar(field: &Field) -> bool {
    match field.field_type {
        FieldType::Base(_) => true,
        _ => false,
    }
}

fn column_type(field: &Field) -> ColumnType {
    column_type_for_scalar_type(scalar_type(field))
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

fn column_type_for_scalar_type(scalar_type: &ScalarType) -> ColumnType {
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
