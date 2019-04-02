use super::{filter::IntoFilter, order_by};
use crate::protobuf::QueryArguments;
use prisma_models::ModelRef;

pub fn into_model_query_arguments(model: ModelRef, args: QueryArguments) -> connector::QueryArguments {
    connector::QueryArguments {
        skip: args.skip,
        after: args.after.map(|x| x.into()),
        first: args.first,
        before: args.before.map(|x| x.into()),
        last: args.last,
        filter: args.filter.map(|x| x.into_filter(model.clone())),
        order_by: args.order_by.map(|x| order_by::into_model_order_by(model.clone(), x)),
    }
}
