use crate::*;
use datamodel::dml;
use itertools::Itertools;

pub trait DatamodelConverter {
    fn convert(datamodel: &dml::Datamodel) -> InternalDataModelTemplate;
}

pub struct DatamodelConverterImpl<'a> {
    datamodel: &'a dml::Datamodel,
    relations: Vec<TempRelationHolder>,
}

#[allow(unused)]
impl<'a> DatamodelConverter for DatamodelConverterImpl<'a> {
    fn convert(datamodel: &dml::Datamodel) -> InternalDataModelTemplate {
        DatamodelConverterImpl::new(datamodel).convert_internal()
    }
}

#[allow(unused)]
impl<'a> DatamodelConverterImpl<'a> {
    fn new(datamodel: &dml::Datamodel) -> DatamodelConverterImpl {
        DatamodelConverterImpl {
            datamodel,
            relations: Self::calculate_relations(datamodel),
        }
    }

    fn convert_internal(&self) -> InternalDataModelTemplate {
        InternalDataModelTemplate {
            models: self.convert_models(),
            relations: self.convert_relations(),
            enums: self.convert_enums(),
            version: None,
        }
    }

    fn convert_enums(&self) -> Vec<InternalEnum> {
        self.datamodel
            .enums()
            .map(|e| InternalEnum {
                name: e.name.clone(),
                values: e.values.clone(),
            })
            .collect()
    }

    fn convert_models(&self) -> Vec<ModelTemplate> {
        self.datamodel
            .models()
            .map(|model| ModelTemplate {
                name: model.name.clone(),
                stable_identifier: "".to_string(),
                is_embedded: model.is_embedded,
                fields: self.convert_fields(model),
                manifestation: model.database_name.clone().map(|n| ModelManifestation { db_name: n }),
            })
            .collect()
    }

    fn convert_fields(&self, model: &dml::Model) -> Vec<FieldTemplate> {
        model
            .fields()
            .map(|field| match field.type_identifier() {
                TypeIdentifier::Relation => {
                    let relation = self
                        .relations
                        .iter()
                        .find(|r| r.is_for_model_and_field(model, field))
                        .expect("Did not find a relation for those fields");

                    FieldTemplate::Relation(RelationFieldTemplate {
                        name: field.name.clone(),
                        type_identifier: field.type_identifier(),
                        is_required: field.is_required(),
                        is_list: field.is_list(),
                        is_unique: field.is_unique(),
                        is_hidden: false,
                        is_auto_generated: field.is_auto_generated(),
                        manifestation: field.manifestation(),
                        relation_name: relation.name(),
                        relation_side: relation.relation_side(field),
                    })
                }
                ti => FieldTemplate::Scalar(ScalarFieldTemplate {
                    name: field.name.clone(),
                    type_identifier: field.type_identifier(),
                    is_required: field.is_required(),
                    is_list: field.is_list(),
                    is_unique: field.is_unique(),
                    is_hidden: false,
                    is_auto_generated: field.is_auto_generated(),
                    manifestation: field.manifestation(),
                    behaviour: field.behaviour(),
                    default_value: None, // TODO: Handle default values.
                    internal_enum: None, // TODO: Handle enums.
                }),
            })
            .collect()
    }

    fn convert_relations(&self) -> Vec<RelationTemplate> {
        self.relations
            .iter()
            .map(|r| RelationTemplate {
                name: r.name(),
                model_a_on_delete: OnDelete::SetNull,
                model_b_on_delete: OnDelete::SetNull,
                manifestation: Some(r.manifestation()),
                model_a_name: r.model_a.name.clone(),
                model_b_name: r.model_b.name.clone(),
            })
            .collect()
    }

    fn calculate_relations(datamodel: &dml::Datamodel) -> Vec<TempRelationHolder> {
        let mut result = Vec::new();
        for model in datamodel.models() {
            for field in model.fields() {
                match &field.field_type {
                    dml::FieldType::Relation(relation_info) => {
                        let dml::RelationInfo {
                            to,
                            to_fields,
                            name,
                            on_delete: _,
                        } = relation_info;
                        let related_model = datamodel.find_model(&to).unwrap();
                        // TODO: handle case of implicit back relation field
                        let related_field = related_model
                            .fields()
                            .find(|f| related_type(f) == Some(model.name.to_string()))
                            .unwrap()
                            .clone();

                        let related_field_info = match &related_field.field_type {
                            dml::FieldType::Relation(info) => info,
                            _ => panic!("this was not a relation field"),
                        };

                        let (model_a, model_b, field_a, field_b) = match () {
                            _ if &model.name < &related_model.name => (
                                model.clone(),
                                related_model.clone(),
                                field.clone(),
                                related_field.clone(),
                            ),
                            _ if &related_model.name < &model.name => (
                                related_model.clone(),
                                model.clone(),
                                related_field.clone(),
                                field.clone(),
                            ),
                            _ => (
                                model.clone(),
                                related_model.clone(),
                                field.clone(),
                                related_field.clone(),
                            ),
                        };
                        let inline_on_model_a = RelationLinkManifestation::Inline(InlineRelation {
                            in_table_of_model_name: model_a.name.clone(),
                            referencing_column: field_a.final_db_name(),
                        });
                        let inline_on_model_b = RelationLinkManifestation::Inline(InlineRelation {
                            in_table_of_model_name: model_b.name.clone(),
                            referencing_column: field_b.final_db_name(),
                        });
                        let inline_on_this_model = RelationLinkManifestation::Inline(InlineRelation {
                            in_table_of_model_name: model.name.clone(),
                            referencing_column: field.final_db_name(),
                        });
                        let inline_on_related_model = RelationLinkManifestation::Inline(InlineRelation {
                            in_table_of_model_name: related_model.name.clone(),
                            referencing_column: related_field.final_db_name(),
                        });

                        let manifestation = match (field_a.is_list(), field_b.is_list()) {
                            (true, true) => RelationLinkManifestation::RelationTable(RelationTable {
                                table: "".to_string(),
                                model_a_column: "A".to_string(),
                                model_b_column: "B".to_string(),
                                id_column: None,
                            }),
                            (false, true) => inline_on_model_a,
                            (true, false) => inline_on_model_b,
                            // TODO: to_fields is now a list, please fix this line.
                            (false, false) => match (to_fields.first(), &related_field_info.to_fields.first()) {
                                (Some(_), None) => inline_on_this_model,
                                (None, Some(_)) => inline_on_related_model,
                                (None, None) => {
                                    if model_a.name < model_b.name {
                                        inline_on_model_a
                                    } else {
                                        inline_on_model_b
                                    }
                                }
                                (Some(_), Some(_)) => {
                                    panic!("It's not allowed that both sides of a relation specify the inline policy")
                                }
                            },
                        };

                        result.push(TempRelationHolder {
                            name: name.clone(),
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

#[derive(Debug, Clone)]
struct TempRelationHolder {
    name: Option<String>,
    model_a: dml::Model,
    model_b: dml::Model,
    field_a: dml::Field,
    field_b: dml::Field,
    manifestation: RelationLinkManifestation,
}

#[allow(unused)]
impl TempRelationHolder {
    fn name(&self) -> String {
        // TODO: must replicate behaviour of `generateRelationName` from `SchemaInferrer`
        match &self.name {
            Some(name) => name.clone(),
            None => format!("{}To{}", &self.model_a.name, &self.model_b.name),
        }
    }

    fn table_name(&self) -> String {
        format!("_{}", self.name())
    }

    fn is_many_to_many(&self) -> bool {
        self.field_a.is_list() && self.field_b.is_list()
    }

    fn is_for_model_and_field(&self, model: &dml::Model, field: &dml::Field) -> bool {
        (&self.model_a == model && &self.field_a == field) || (&self.model_b == model && &self.field_b == field)
    }

    fn relation_side(&self, field: &dml::Field) -> RelationSide {
        if field == &self.field_a {
            RelationSide::A
        } else if field == &self.field_b {
            RelationSide::B
        } else {
            panic!("this field is not part of hte relations")
        }
    }

    fn manifestation(&self) -> RelationLinkManifestation {
        match &self.manifestation {
            RelationLinkManifestation::RelationTable(rt) => {
                let mut cloned = rt.clone();
                cloned.table = self.table_name();
                RelationLinkManifestation::RelationTable(cloned)
            }
            m => m.clone(),
        }
    }
}

fn related_type(field: &dml::Field) -> Option<String> {
    match &field.field_type {
        dml::FieldType::Relation(relation_info) => Some(relation_info.to.to_string()),
        _ => None,
    }
}

trait DatamodelFieldExtensions {
    fn type_identifier(&self) -> TypeIdentifier;
    fn is_required(&self) -> bool;
    fn is_list(&self) -> bool;
    fn is_unique(&self) -> bool;
    fn is_auto_generated(&self) -> bool;
    fn manifestation(&self) -> Option<FieldManifestation>;
    fn behaviour(&self) -> Option<FieldBehaviour>;
    fn final_db_name(&self) -> String;
}

impl DatamodelFieldExtensions for dml::Field {
    fn type_identifier(&self) -> TypeIdentifier {
        match self.field_type {
            dml::FieldType::Enum(_) => TypeIdentifier::Enum,
            dml::FieldType::Relation(_) => TypeIdentifier::Relation,
            dml::FieldType::Base(scalar) => match scalar {
                dml::ScalarType::Boolean => TypeIdentifier::Boolean,
                dml::ScalarType::DateTime => TypeIdentifier::DateTime,
                dml::ScalarType::Decimal => TypeIdentifier::Float,
                dml::ScalarType::Float => TypeIdentifier::Float,
                dml::ScalarType::Int => TypeIdentifier::Int,
                dml::ScalarType::String => TypeIdentifier::String,
            },
            dml::FieldType::ConnectorSpecific {
                base_type: _,
                connector_type: _,
            } => unimplemented!("Connector Specific types are not supported"),
        }
    }

    fn is_required(&self) -> bool {
        self.arity == dml::FieldArity::Required
    }
    fn is_list(&self) -> bool {
        self.arity == dml::FieldArity::List
    }
    fn is_unique(&self) -> bool {
        self.is_unique
    }
    fn is_auto_generated(&self) -> bool {
        // TODO: is true when the value gets auto generated by the db. Must be true for int ids that are backed by a sequence.
        false
    }

    fn manifestation(&self) -> Option<FieldManifestation> {
        self.database_name.clone().map(|n| FieldManifestation { db_name: n })
    }

    fn behaviour(&self) -> Option<FieldBehaviour> {
        // TODO: implement this properly once this is specced for the datamodel
        self.id_info.as_ref().map(|_| FieldBehaviour::Id {
            strategy: IdStrategy::None,
            sequence: None,
        })
    }

    fn final_db_name(&self) -> String {
        self.database_name.clone().unwrap_or_else(|| self.name.clone())
    }
}
