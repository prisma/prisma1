use database_inspector::relational::{
    ColumnInfo, ColumnType, IndexInfo, SchemaInfo as DatabaseSchema, TableInfo, TableRelationInfo,
};
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
        let mut schema = DatabaseSchema::new();

        // TODO: It's probably better to move this into one loop.
        schema.merge(self.calculate_model_tables());
        schema.merge(self.calculate_scalar_list_tables());
        schema.merge(self.calculate_relation_tables());

        schema
    }

    /// Generates tables for models, includes inline relations.
    fn calculate_model_tables(&self) -> DatabaseSchema {
        let mut schema = DatabaseSchema::new();

        for model in self.data_model.models() {
            let mut table = TableInfo {
                name: String::from(model.db_name()),
                columns: Vec::new(),
                indexes: Vec::new(),
                primary_key: Some(IndexInfo::new_pk(&[model.id_field().db_name()])),
            };

            for field in model.fields() {
                // TODO: Removing all list fields breaks for scalar fields if the
                // db supports embedded lists.
                if field.arity == FieldArity::List {
                    continue;
                }

                match field.field_type {
                    FieldType::Base(scalar_type) => table.columns.push(ColumnInfo::new(
                        field.db_name(),
                        column_type_for_scalar_type(&scalar_type),
                        field.arity != FieldArity::Required,
                    )),
                    FieldType::Enum(enum_name) => unimplemented!("No Enum support yet."),
                    FieldType::Relation(relation_info) => {
                        let related = self
                            .data_model
                            .find_model(&relation_info.to)
                            .expect("Invalid related model. This should never happen.");
                        let related_id_field = related.id_field();
                        let to_field = match relation_info.to_field {
                            Some(name) => related.find_field(&name),
                            None => None,
                        };

                        // We removed all lists above,
                        // therefore we can assume that we are never going to find an n:m field here,
                        // and field is always the FK embedder.

                        // Self Relations: We can embed, if either:
                        // * There is no related field.
                        // * The related field is a list.
                        // * Our name is smaller than the related fields name (this is a tie breaker for an 1:1 self relation)
                        let is_self_relation = related.name == model.name;
                        let can_embed_self_relation = to_field.is_none()
                            || to_field.unwrap().arity == FieldArity::List
                            || field.name < to_field.unwrap().name;

                        // 1:1 Relations: We embed if our name is smaller (tie breaker).
                        let is_one_one_relation = to_field.is_some() && to_field.unwrap().arity != FieldArity::List;
                        let can_embed_one_one_relation = is_one_one_relation && field.name < to_field.unwrap().name;

                        // We can embed the ID into field if either:
                        // * This is a self relation field and the check above passed.
                        // * This is a 1:1 relation field and the check above passed.
                        // * This is not a self relation or a 1:1 relation.
                        if (is_self_relation && can_embed_self_relation)
                            || (is_one_one_relation && can_embed_one_one_relation)
                            || (!is_self_relation && !is_one_one_relation)
                        {
                            table.columns.push(ColumnInfo::new(
                                field.db_name(),
                                column_type(related_id_field),
                                field.arity != FieldArity::Required,
                            ));

                            let fk = TableRelationInfo::new(
                                related.db_name(),
                                related_id_field.db_name(),
                                &table.name,
                                field.db_name(),
                            );
                            schema.relations.push(fk);
                        }
                    }
                    FieldType::ConnectorSpecific {
                        base_type: _,
                        connector_type: _,
                    } => unimplemented!("No connector specific support yet."),
                }
            }

            schema.tables.push(table);
        }

        schema
    }

    /// Generates tables for scalar list fields.
    fn calculate_scalar_list_tables(&self) -> DatabaseSchema {
        let mut tables = Vec::new();
        let mut relations = Vec::new();

        let node_id_field = "nodeId";
        let position_field = "position";
        let value_field = "value";

        for model in self.data_model.models() {
            let list_fields: Vec<&Field> = model
                .fields()
                .filter(|f| f.arity == FieldArity::List && is_scalar(f))
                .collect();

            for field in list_fields {
                let id_field = model.id_field();

                let table = TableInfo {
                    name: format!("{}_{}", model.db_name(), field.db_name()),
                    columns: vec![
                        ColumnInfo::new(node_id_field, column_type(&id_field), false),
                        ColumnInfo::new(position_field, ColumnType::Int, false),
                        ColumnInfo::new(value_field, column_type(&field), false),
                    ],
                    indexes: Vec::new(),
                    primary_key: Some(IndexInfo::new_pk(&[node_id_field, position_field])),
                };
                tables.push(table);

                let relation =
                    TableRelationInfo::new(model.db_name(), id_field.db_name(), &table.name, node_id_field);
                relations.push(relation);
            }
        }

        DatabaseSchema {
            tables,
            relations,
            enums: vec![],
            sequences: vec![],
        }
    }

    //// Generates tables for Many to Many relations.
    fn calculate_relation_tables(&self) -> DatabaseSchema {
        let mut schema = DatabaseSchema::new();

        for model in self.data_model.models() {
            for field in model.fields() {
                // We are only interested in lists for finding n:m rels.
                if field.arity != FieldArity::List {
                    continue;
                }

                match field.field_type {
                    FieldType::Relation(relation_info) => {
                        let related = self
                            .data_model
                            .find_model(&relation_info.to)
                            .expect("Invalid related model. This should never happen.");

                        let my_id_field = model.id_field();
                        let related_id_field = related.id_field();

                        let to_field = match relation_info.to_field {
                            Some(name) => related.find_field(&name),
                            None => None,
                        };

                        let is_many_many_relationship = to_field.is_some() && to_field.unwrap().is_list();
                        // This is a tie-breaker again.
                        // TODO: This can be very unstable with renaming.
                        let can_create = field.name < to_field.unwrap().name;

                        if !is_many_many_relationship || !can_create {
                            continue;
                        }

                        let relation_name = relation_info
                            .name
                            .expect("Cannot create many to many relation with out a name");

                        // Create Relation Table
                        let table = TableInfo {
                            name: String::from(relation_name),
                            columns: vec![
                                ColumnInfo::new("A", column_type(my_id_field), false),
                                ColumnInfo::new("B", column_type(related_id_field), false),
                            ],
                            indexes: Vec::new(),
                            primary_key: Some(IndexInfo::new_pk(&["A", "B"])),
                        };

                        schema.tables.push(table);

                        // Create FKs
                        let fk1 = TableRelationInfo::new(&relation_name, "A", model.db_name(), my_id_field.db_name());
                        schema.relations.push(fk1);
                        let fk2 =
                            TableRelationInfo::new(&relation_name, "B", related.db_name(), related_id_field.db_name());
                        schema.relations.push(fk2);
                    }
                    _ => {}
                }
            }
        }

        schema
    }
}

// TODO: Move all this helper traits to datamodel.
trait ModelExtensions {
    fn id_field(&self) -> &Field;

    fn db_name(&self) -> &str;
}

impl ModelExtensions for Model {
    // todo: find actual id field
    // TODO: This should be done by the validator on datamodel load.
    fn id_field(&self) -> &Field {
        self.fields().find(|f| f.is_id()).unwrap()
    }

    fn db_name(&self) -> &str {
        &self.database_name.unwrap_or_else(|| self.name)
    }
}

trait FieldExtensions {
    fn is_id(&self) -> bool;

    fn is_list(&self) -> bool;

    fn is_required(&self) -> bool;

    fn db_name(&self) -> &str;
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

    fn db_name(&self) -> &str {
        &self.database_name.clone().unwrap_or_else(|| self.name.clone())
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

// TODO: Move to dml::FieldType.
fn scalar_type(field: &Field) -> &ScalarType {
    match &field.field_type {
        FieldType::Base(ref scalar) => scalar,
        x => panic!(format!(
            "Only scalar types are suported here. Type is {:?} on field {}.",
            x, field.name
        )),
    }
}

fn column_type(field: &Field) -> ColumnType {
    column_type_for_scalar_type(scalar_type(field))
}

fn column_type_for_scalar_type(scalar_type: &ScalarType) -> ColumnType {
    match scalar_type {
        ScalarType::Int => ColumnType::Int,
        ScalarType::Float => ColumnType::Float,
        ScalarType::Boolean => ColumnType::Boolean,
        ScalarType::String => ColumnType::String,
        ScalarType::DateTime => ColumnType::DateTime,
        ScalarType::Decimal => unimplemented!("Decimal Column Type is Unimplemented"),
    }
}
