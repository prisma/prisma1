mod many;
mod many_relation;
mod one;
mod one_relation;

pub use many::*;
pub use many_relation::*;
pub use one::*;
pub use one_relation::*;

use crate::query_builders::QueryBuilderResult;
use connector::read_ast::ReadQuery;
use prisma_models::{SelectedField, SelectedFields};

pub trait Builder {
    fn build(self) -> QueryBuilderResult<ReadQuery>;
}

pub enum ReadQueryBuilder {
    ReadOneRecordBuilder(ReadOneRecordBuilder),
    ReadManyRecordsBuilder(ReadManyRecordsBuilder),
    ReadOneRelationRecordBuilder(ReadOneRelationRecordBuilder),
    ReadManyRelationRecordsBuilder(ReadManyRelationRecordsBuilder),
}

impl Builder for ReadQueryBuilder {
    fn build(self) -> QueryBuilderResult<ReadQuery> {
        match self {
            ReadQueryBuilder::ReadOneRecordBuilder(b) => b.build(),
            ReadQueryBuilder::ReadManyRecordsBuilder(b) => b.build(),
            ReadQueryBuilder::ReadOneRelationRecordBuilder(b) => b.build(),
            ReadQueryBuilder::ReadManyRelationRecordsBuilder(b) => b.build(),
        }
    }
}

///// Get all selected fields from a model
//pub fn collect_selected_fields<I: Into<Option<RelationFieldRef>>>(
//    model: ModelRef,
//    field: &Field,
//    parent: I,
//) -> QueryBuilderResult<SelectedFields> {
//    field
//        .selection_set
//        .items
//        .iter()
//        .filter_map(|i| {
//            if let Selection::Field(f) = i {
//                // We have to make sure the selected field exists in some form.
//                let field = model.fields().find_from_all(&f.name);
//                match field {
//                    Ok(ModelField::Scalar(field)) => Some(Ok(SelectedField::Scalar(SelectedScalarField {
//                        field: Arc::clone(&field),
//                        implicit: false,
//                    }))),
//                    // Relation fields are not handled here, but in nested queries
//                    Ok(ModelField::Relation(field)) => Some(Ok(SelectedField::Relation(SelectedRelationField {
//                        field: Arc::clone(&field),
//                        selected_fields: SelectedFields::new(vec![], None),
//                    }))),
//                    _ => Some(Err(CoreError::LegacyQueryValidationError(format!(
//                        "Selected field {} not found on model {}",
//                        f.name, model.name,
//                    )))),
//                }
//            } else {
//                Some(Err(CoreError::UnsupportedFeatureError(
//                    "Fragments and inline fragment spreads.".into(),
//                )))
//            }
//        })
//        .collect::<CoreResult<Vec<_>>>()
//        .map(|sf| SelectedFields::new(sf, parent.into()))
//}
