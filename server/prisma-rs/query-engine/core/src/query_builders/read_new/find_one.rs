use connector::read_ast::ReadQuery;
use crate::query_builders::query_builder::{QueryBuilderResult, ParsedField};
use prisma_models::ModelRef;

pub struct FindOneQueryBuilder;

impl FindOneQueryBuilder {
    pub fn build(parsed_field: ParsedField, model: ModelRef) -> QueryBuilderResult<ReadQuery> {
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