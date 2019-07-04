use connector::{
    read_ast::ReadQuery,
    filter::RecordFinder
};
use crate::query_builders::{QueryBuilderResult, ParsedField, utils};
use prisma_models::{ModelRef, Field, SelectedScalarField, SelectedRelationField};
use std::convert::TryInto;
use super::*;
use std::sync::Arc;
use connector::read_ast::RecordQuery;

pub struct ReadOneRecordBuilder {
    field: ParsedField,
    model: ModelRef,
}

impl ReadOneRecordBuilder {
    pub fn new(field: ParsedField, model: ModelRef) -> Self {
        Self { field, model }
    }
}

impl ReadQueryBuilder for ReadOneRecordBuilder {
    /// Builds a read query tree from a parsed top-level field of a query
    /// Unwraps are safe because of query validation that ensures conformity to the query schema.
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        let arguments = self.field.arguments;
        let record_finder = utils::extract_record_finder(arguments, &self.model)?;

        // WIP nested queries
//        let nested_builders = utils::collect_nested_queries(Arc::clone(&model), field, model.internal_data_model())?;
//        let nested = utils::build_nested_queries(nested_builders)?;

        // Read query requires a selection set.
        let sub_selections = self.field.sub_selections.unwrap().fields;
        let selection_order: Vec<String> = sub_selections
            .iter()
            .map(|selected_field| selected_field.alias.clone().unwrap_or_else(|| selected_field.name.clone())).collect();

        let model_fields = self.model.fields();
        let selected_fields = sub_selections.into_iter().map(|selected_field| {
            let model_field = model_fields.find_from_all(&selected_field.name).unwrap();
            match model_field {
                Field::Scalar(ref sf) => SelectedField::Scalar(SelectedScalarField {
                    field: Arc::clone(sf),
                }),
                Field::Relation(ref rf) => SelectedField::Relation(SelectedRelationField {
                    field: Arc::clone(rf),
                    selected_fields: SelectedFields::new(vec![], None),
                })
            }
        }).collect::<Vec<SelectedField>>();

        let selected_fields = SelectedFields::new(selected_fields, None);
        let name = self.field.alias.unwrap_or(self.field.name);

        Ok(ReadQuery::RecordQuery(RecordQuery {
            name,
            record_finder,
            selected_fields,
            nested: vec![],
            selection_order,
        }))
    }
}

//
//pub(crate) fn collect_nested_queries<'field>(
//    model: ModelRef,
//    ast_field: &'field Field,
//    _internal_data_model: InternalDataModelRef,
//) -> CoreResult<Vec<ReadQueryBuilder<'field>>> {
//    ast_field
//        .selection_set
//        .items
//        .iter()
//        .filter_map(|i| {
//            if let Selection::Field(x) = i {
//                let field = &model.fields().find_from_all(&x.name);
//                match &field {
//                    Ok(ModelField::Scalar(_f)) => None,
//                    Ok(ModelField::Relation(f)) => {
//                        let model = f.related_model();
//                        let parent = Some(Arc::clone(&f));
//
//                        ReadQueryBuilder::infer_nested(&model, x, parent).map(|r| Ok(r))
//                    }
//                    _ => Some(Err(CoreError::LegacyQueryValidationError(format!(
//                        "Selected field {} not found on model {}",
//                        x.name, model.name,
//                    )))),
//                }
//            } else {
//                Some(Err(CoreError::UnsupportedFeatureError(
//                    "Fragments and inline fragment spreads.".into(),
//                )))
//            }
//        })
//        .collect()
//}
//
//pub(crate) fn build_nested_queries(builders: Vec<ReadQueryBuilder>) -> CoreResult<Vec<ReadQuery>> {
//    builders
//        .into_iter()
//        .map(|b| match b {
//            ReadQueryBuilder::OneRelation(b) => Ok(ReadQuery::RelatedRecordQuery(b.build()?)),
//            ReadQueryBuilder::ManyRelation(b) => Ok(ReadQuery::ManyRelatedRecordsQuery(b.build()?)),
//            _ => unreachable!(),
//        })
//        .collect()
//}