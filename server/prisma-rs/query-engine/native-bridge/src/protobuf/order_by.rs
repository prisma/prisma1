use prisma_models::{ModelRef, OrderBy};

pub fn into_model_order_by(model: ModelRef, ord: crate::protobuf::prisma::OrderBy) -> OrderBy {
    let field = model.fields().find_from_scalar(&ord.scalar_field).unwrap();

    OrderBy {
        field,
        sort_order: ord.sort_order().into(),
    }
}
