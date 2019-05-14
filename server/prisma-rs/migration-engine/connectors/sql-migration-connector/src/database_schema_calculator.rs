use database_inspector::*;
use datamodel::*;
use std::collections::HashSet;

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
        let mut relation_tables = self.calculate_relation_tables();

        tables.append(&mut model_tables);
        tables.append(&mut scalar_list_tables);
        tables.append(&mut relation_tables);

        DatabaseSchema { tables }
    }

    fn calculate_model_tables(&self) -> Vec<Table> {
        self.data_model
            .models()
            .map(|model| {
                let columns = model
                    .fields()
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
                    name: model.name.clone(),
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
                .fields()
                .filter(|f| f.arity == FieldArity::List && is_scalar(f))
                .collect();
            for field in list_fields {
                let id_field = id_field(&model); // todo: find actual id field
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

    fn calculate_relation_tables(&self) -> Vec<Table> {
        let mut result = Vec::new();
        for relation in self.calculate_relations().iter() {
            match &relation.manifestation {
                RelationManifestation::Table {
                    model_a_column,
                    model_b_column,
                } if relation.is_many_to_many() => {
                    let table = Table {
                        name: relation.table_name(),
                        columns: vec![
                            Column::with_foreign_key(
                                model_a_column.to_string(),
                                column_type(&scalar_type(id_field(&relation.model_a))),
                                true,
                                ForeignKey {
                                    table: relation.model_a.name.to_string(),
                                    column: id_field(&relation.model_a).name.to_string(),
                                },
                            ),
                            Column::with_foreign_key(
                                model_b_column.to_string(),
                                column_type(&scalar_type(id_field(&relation.model_b))),
                                true,
                                ForeignKey {
                                    table: relation.model_b.name.to_string(),
                                    column: id_field(&relation.model_b).name.to_string(),
                                },
                            ),
                        ],
                        indexes: Vec::new(),
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
                            to_field,
                            name,
                            on_delete,
                        } = relation_info;
                        let related_model = self.data_model.find_model(&to).unwrap();
                        // TODO: handle case of implicit back relation field
                        let related_field = related_model
                            .fields()
                            .find(|f| related_type(f) == Some(model.name.to_string()))
                            .unwrap()
                            .clone();
                        let manifestation = RelationManifestation::Table {
                            model_a_column: "A".to_string(),
                            model_b_column: "B".to_string(),
                        };
                        let (model_a, model_b, field_a, field_b) = match () {
                            _ if &model.name < &related_model.name => {
                                (model.clone(), related_model.clone(), field.clone(), related_field)
                            }
                            _ if &related_model.name < &model.name => {
                                (related_model.clone(), model.clone(), related_field, field.clone())
                            }
                            _ => (model.clone(), related_model.clone(), field.clone(), related_field),
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
        result.dedup_by(|rel1, rel2| rel1 == rel2);
        result
    }
}

#[derive(PartialEq)]
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

    fn is_many_to_many(&self) -> bool {
        self.field_a.arity == FieldArity::List && self.field_b.arity == FieldArity::List
    }
}

#[derive(PartialEq)]
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

fn id_field(model: &Model) -> &Field {
    model.fields().next().clone().unwrap()
}

fn related_type(field: &Field) -> Option<String> {
    match &field.field_type {
        FieldType::Relation(relation_info) => {
            let RelationInfo {
                to,
                to_field,
                name,
                on_delete,
            } = relation_info;
            Some(to.to_string())
        }
        _ => None,
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
