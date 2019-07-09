use super::*;
use crate::query_builders::{utils, ParsedField, ParsedObject, QueryBuilderResult};
use connector::read_ast::{ReadQuery, RecordQuery};
use prisma_models::{Field, ModelRef, RelationFieldRef, SelectedRelationField, SelectedScalarField};
use std::sync::Arc;

pub struct ReadOneRecordBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl ReadOneRecordBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl Builder for ReadOneRecordBuilder {
    /// Builds a read query tree from a parsed top-level field of a query
    /// Unwraps are safe because of query validation that ensures conformity to the query schema.
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        let arguments = self.field.arguments;
        let record_finder = utils::extract_record_finder(arguments, &self.model)?;
        let name = self.field.alias.unwrap_or(self.field.name);
        let model_fields = self.model.fields();

        // Read query requires a selection set.
        let sub_selections = self.field.sub_selections.unwrap().fields;
        let selection_order: Vec<String> = sub_selections
            .iter()
            .map(|selected_field| {
                selected_field
                    .alias
                    .clone()
                    .unwrap_or_else(|| selected_field.name.clone())
            })
            .collect();

        let selected_fields = sub_selections
            .iter()
            .map(|selected_field| {
                let model_field = model_fields.find_from_all(&selected_field.name).unwrap();
                match model_field {
                    Field::Scalar(ref sf) => SelectedField::Scalar(SelectedScalarField { field: Arc::clone(sf) }),
                    Field::Relation(ref rf) => SelectedField::Relation(SelectedRelationField {
                        field: Arc::clone(rf),
                        selected_fields: SelectedFields::new(vec![], None),
                    }),
                }
            })
            .collect::<Vec<SelectedField>>();

        let selected_fields = SelectedFields::new(selected_fields, None);

        // Build nested queries
        let nested = sub_selections
            .into_iter()
            .filter_map(|selected_field| {
                let model_field = model_fields.find_from_all(&selected_field.name).unwrap();
                match model_field {
                    Field::Scalar(_) => None,
                    Field::Relation(ref rf) => {
                        let model = rf.related_model();
                        let parent = Arc::clone(&rf);

                        Some(infer_nested(selected_field, &model, parent))
                    }
                }
            })
            .collect::<Vec<ReadQueryBuilder>>()
            .into_iter()
            .map(|builder| builder.build())
            .collect::<QueryBuilderResult<Vec<ReadQuery>>>()?;

        Ok(ReadQuery::RecordQuery(RecordQuery {
            name,
            record_finder,
            selected_fields,
            nested,
            selection_order,
        }))
    }
}

fn infer_nested(field: ParsedField, model: &ModelRef, parent: RelationFieldRef) -> ReadQueryBuilder {
    if parent.is_list {
        ReadQueryBuilder::ReadManyRelationRecordsBuilder(ReadManyRelationRecordsBuilder::new(
            Arc::clone(model),
            Arc::clone(&parent),
            field,
        ))
    } else {
        ReadQueryBuilder::ReadOneRelationRecordBuilder(ReadOneRelationRecordBuilder::new(
            Arc::clone(model),
            Arc::clone(&parent),
            field,
        ))
    }
}

/// Returns: Selection order, selected model fields, nested query builders/
// fn process_selections(
//     model: &ModelRef,
//     from: Vec<ParsedField>,
// ) -> QueryBuilderResult<(Vec<String>, SelectedFields, Vec<ReadQueryBuilders>)> {
//     let model_fields = self.model.fields();
//     let selection_order: Vec<String> = vec![];
//     let selected_fields: Vec<SelectedField> = vec![];
//     let nested_builders: Vec<ReadQueryBuilders> = vec![];

//     unimplemented!()
// }

fn collect_nested_queries(model: &ModelRef, from: ParsedObject) -> QueryBuilderResult<Vec<ReadQuery>> {
    from.fields.iter().filter_map(|f| unimplemented!()).collect()
}
