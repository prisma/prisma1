use connector::{
    error::ConnectorError,
    mutaction::{DeleteNode, DeleteNodes, NestedDeleteNode, NestedDeleteNodes},
    ConnectorResult,
};
use prisma_models::prelude::*;
use prisma_query::ast::*;
use std::sync::Arc;

pub trait DeleteActions {
    fn model(&self) -> ModelRef;

    fn check_relation_violations<F>(&self, ids: &[&GraphqlId], f: F) -> ConnectorResult<()>
    where
        F: Fn(Select) -> ConnectorResult<Option<GraphqlId>>,
    {
        let model = self.model();

        for rf in model.schema().fields_requiring_model(model) {
            let relation = rf.relation();

            let condition = rf
                .opposite_column()
                .in_selection(ids.to_vec())
                .and(rf.relation_column().is_not_null());

            let select = Select::from_table(relation.relation_table())
                .column(rf.opposite_column())
                .so_that(condition);

            if let Some(_) = f(select)? {
                return Err(ConnectorError::RelationViolation {
                    relation_name: relation.name.clone(),
                    model_a_name: relation.model_a().name.clone(),
                    model_b_name: relation.model_b().name.clone(),
                });
            }
        }

        Ok(())
    }
}

impl DeleteActions for NestedDeleteNode {
    fn model(&self) -> ModelRef {
        self.relation_field.related_model()
    }
}

impl DeleteActions for NestedDeleteNodes {
    fn model(&self) -> ModelRef {
        self.relation_field.model()
    }
}

impl DeleteActions for DeleteNode {
    fn model(&self) -> ModelRef {
        self.where_.field.model()
    }
}

impl DeleteActions for DeleteNodes {
    fn model(&self) -> ModelRef {
        Arc::clone(&self.model)
    }
}
