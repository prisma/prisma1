mod many;
mod many_relation;
mod one;
mod one_relation;

pub use many::*;
pub use many_relation::*;
pub use one::*;
pub use one_relation::*;

use crate::query_builders::{ParsedField, QueryBuilderResult, Builder};
use connector::read_ast::ReadQuery;
use prisma_models::{
    Field, ModelRef, RelationFieldRef, SelectedField, SelectedFields, SelectedRelationField, SelectedScalarField,
};
use std::sync::Arc;

pub enum ReadQueryBuilder {
    ReadOneRecordBuilder(ReadOneRecordBuilder),
    ReadManyRecordsBuilder(ReadManyRecordsBuilder),
    ReadOneRelationRecordBuilder(ReadOneRelationRecordBuilder),
    ReadManyRelationRecordsBuilder(ReadManyRelationRecordsBuilder),
}

impl Builder<ReadQuery> for ReadQueryBuilder {
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        match self {
            ReadQueryBuilder::ReadOneRecordBuilder(b) => b.build(),
            ReadQueryBuilder::ReadManyRecordsBuilder(b) => b.build(),
            ReadQueryBuilder::ReadOneRelationRecordBuilder(b) => b.build(),
            ReadQueryBuilder::ReadManyRelationRecordsBuilder(b) => b.build(),
        }
    }
}

pub fn collect_selection_order(from: &[ParsedField]) -> Vec<String> {
    from.into_iter()
        .map(|selected_field| {
            selected_field
                .alias
                .clone()
                .unwrap_or_else(|| selected_field.name.clone())
        })
        .collect()
}

pub fn collect_selected_fields(
    from: &[ParsedField],
    model: &ModelRef,
    parent: Option<RelationFieldRef>,
) -> SelectedFields {
    let selected_fields = from
        .into_iter()
        .map(|selected_field| {
            let model_field = model.fields().find_from_all(&selected_field.name).unwrap();
            match model_field {
                Field::Scalar(ref sf) => SelectedField::Scalar(SelectedScalarField { field: Arc::clone(sf) }),
                Field::Relation(ref rf) => SelectedField::Relation(SelectedRelationField {
                    field: Arc::clone(rf),
                    selected_fields: SelectedFields::new(vec![], None), // todo None here correct?
                }),
            }
        })
        .collect::<Vec<SelectedField>>();

    SelectedFields::new(selected_fields, parent)
}

pub fn collect_nested_queries(from: Vec<ParsedField>, model: &ModelRef) -> QueryBuilderResult<Vec<ReadQuery>> {
    from.into_iter()
        .filter_map(|selected_field| {
            let model_field = model.fields().find_from_all(&selected_field.name).unwrap();
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
        .collect::<QueryBuilderResult<Vec<ReadQuery>>>()
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
