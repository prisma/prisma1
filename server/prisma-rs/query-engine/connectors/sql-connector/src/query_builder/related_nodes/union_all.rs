use super::*;
use crate::ordering::Ordering;
use connector::SkipAndLimit;
use prisma_models::prelude::*;
use prisma_query::ast::*;

pub struct RelatedNodesWithUnionAll;

impl RelatedNodesQueryBuilder for RelatedNodesWithUnionAll {
    fn with_pagination<'a>(base: RelatedNodesBaseQuery<'a>) -> Query {
        let distinct_ids = {
            let mut ids = base.from_node_ids.to_vec();
            ids.dedup();

            ids
        };

        let order_columns = Ordering::internal(
            SelectedFields::RELATED_MODEL_ALIAS,
            base.order_by.as_ref(),
            base.is_reverse_order,
        );

        let base_condition = base.condition.and(base.cursor);
        let from_field = base.from_field;

        let base_query = match base.skip_and_limit {
            SkipAndLimit {
                skip,
                limit: Some(limit),
            } => base.query.limit(limit).offset(skip),
            SkipAndLimit { skip, limit: None } => base.query.offset(skip),
        };

        let base_query = order_columns.into_iter().fold(base_query, |acc, ord| acc.order_by(ord));

        let union = distinct_ids.into_iter().fold(UnionAll::default(), |acc, id| {
            let conditions = base_condition
                .clone()
                .and(from_field.relation_column().table(Relation::TABLE_ALIAS).equals(id));

            acc.add(base_query.clone().so_that(conditions))
        });

        Query::from(union)
    }
}
