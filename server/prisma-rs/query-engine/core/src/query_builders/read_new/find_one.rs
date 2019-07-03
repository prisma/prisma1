use connector::{
    read_ast::ReadQuery,
    filter::RecordFinder
};
use crate::query_builders::{QueryBuilderResult, ParsedField};
use prisma_models::ModelRef;
use std::convert::TryInto;

pub struct FindOneQueryBuilder;

impl FindOneQueryBuilder {
    /// Builds a read query tree from a parsed top-level field of a query
    /// Unwraps are safe because of query validation that ensures conformity to the query schema.
    pub fn build(parsed_field: ParsedField, model: ModelRef) -> QueryBuilderResult<ReadQuery> {
        let record_finder = parsed_field.extract_record_finder(&model)?;


//        let nested_builders = utils::collect_nested_queries(Arc::clone(&model), field, model.internal_data_model())?;
//        let nested = utils::build_nested_queries(nested_builders)?;
//
//        let selected_fields = utils::collect_selected_fields(Arc::clone(&model), field, None)?;
//        let selector = utils::extract_record_finder(&field, Arc::clone(&model))?;
//        let name = field.alias.as_ref().unwrap_or(&field.name).clone();
//        let fields = utils::collect_selection_order(&field);
//
//        Ok(RecordQuery {
//            name,
//            selector,
//            selected_fields,
//            nested,
//            fields,
//        })

        unimplemented!()
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