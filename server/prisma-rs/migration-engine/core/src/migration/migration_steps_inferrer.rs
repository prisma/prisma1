use migration_connector::steps::*;
use database_inspector::DatabaseSchema;
use prisma_models::*;

pub trait MigrationStepsInferrer {
    fn infer(next: &Schema, database_schema: &DatabaseSchema) -> Vec<MigrationStep>;
}

pub struct MigrationStepsInferrerImpl<'a> {
    schema: &'a Schema,
    database_schema: &'a DatabaseSchema,
}

impl<'a> MigrationStepsInferrer for MigrationStepsInferrerImpl<'a> {
    fn infer(next: &Schema, database_schema: &DatabaseSchema) -> Vec<MigrationStep> {
        let inferer = MigrationStepsInferrerImpl {
            schema: next,
            database_schema: database_schema,
        };
        inferer.infer()
    }
}

impl<'a> MigrationStepsInferrerImpl<'a> {
    fn infer(&self) -> Vec<MigrationStep> {
        let mut result: Vec<MigrationStep> = vec![];
        let next_models = self.schema.models();
        let mut create_model_steps: Vec<MigrationStep> = next_models
            .iter()
            .filter(|model| self.database_schema.table(model.db_name()).is_none())
            .map(|model| {
                let step = CreateModel {
                    name: model.name.clone(),
                    db_name: model.db_name_opt().map(|x| x.to_string()),
                    embedded: if model.is_embedded {
                        Some(model.is_embedded)
                    } else {
                        None
                    },
                };
                MigrationStep::CreateModel(step)
            })
            .collect();

        let mut create_field_steps: Vec<MigrationStep> = vec![];
        for model in next_models {
            // TODO: also create steps for relation fields
            for field in model.fields().scalar() {
                let step = CreateField {
                    model: model.name.clone(),
                    name: field.name.clone(),
                    tpe: field.type_identifier.user_friendly_type_name(),
                    db_name: field.db_name_opt().map(|f| f.to_string()),
                    default: None,
                    id: None, //field.id_behaviour_clone(),
                    is_created_at: field.is_created_at().as_some_if_true(),
                    is_updated_at: field.is_updated_at().as_some_if_true(),
                    is_list: field.is_list.as_some_if_true(),
                    is_optional: field.is_required.as_some_if_true(),
                    scalar_list: None, //field.scalar_list_behaviour_clone(),
                };
                create_field_steps.push(MigrationStep::CreateField(step))
            }
        }

        let mut create_enum_steps = vec![];
        for prisma_enum in &self.schema.enums {
            let step = CreateEnum {
                name: prisma_enum.name.clone(),
                values: prisma_enum.values.clone(),
            };
            create_enum_steps.push(MigrationStep::CreateEnum(step));
        }

        let mut create_relations = vec![];
        let relations = self.schema.relations();
        for relation in relations {
            let model_a = relation.model_a();
            let model_b = relation.model_b();
            let field_a = relation.field_a();
            let field_b = relation.field_b();

            let step = CreateRelation {
                name: relation.name.clone(),
                model_a: RelationFieldSpec {
                    name: model_a.name.clone(),
                    field: Some(field_a.name.clone()),
                    is_list: field_a.is_list.as_some_if_true(),
                    is_optional: field_a.is_optional().as_some_if_true(),
                    on_delete: None, //Some(relation.model_a_on_delete),
                    inline_link: self.is_inlined_in_model(relation, &model_a).as_some_if_true(),
                },
                model_b: RelationFieldSpec {
                    name: model_b.name.clone(),
                    field: Some(field_b.name.clone()),
                    is_list: field_b.is_list.as_some_if_true(),
                    is_optional: field_b.is_optional().as_some_if_true(),
                    on_delete: None, //Some(relation.model_a_on_delete),
                    inline_link: self.is_inlined_in_model(relation, &model_b).as_some_if_true(),
                },
                table: match relation.manifestation {
                    Some(RelationLinkManifestation::RelationTable(ref mani)) => Some(LinkTableSpec {
                        model_a_column: Some(mani.model_a_column.clone()),
                        model_b_column: Some(mani.model_b_column.clone()),
                    }),
                    _ => None,
                },
            };
            create_relations.push(MigrationStep::CreateRelation(step));
        }

        result.append(&mut create_model_steps);
        result.append(&mut create_field_steps);
        result.append(&mut create_enum_steps);
        result.append(&mut create_relations);
        result
    }

    fn is_inlined_in_model(&self, relation: &RelationRef, model: &ModelRef) -> bool {
        match relation.manifestation {
            Some(RelationLinkManifestation::Inline(ref mani)) => mani.in_table_of_model_name == model.name,
            _ => false,
        }
    }
}

trait ToOption {
    fn as_some_if_true(self) -> Option<bool>;
}

impl ToOption for bool {
    fn as_some_if_true(self) -> Option<bool> {
        if self {
            Some(true)
        } else {
            None
        }
    }
}
