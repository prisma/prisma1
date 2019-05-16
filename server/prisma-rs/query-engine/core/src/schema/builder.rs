use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};
use std::sync::Arc;

pub struct QuerySchemaBuilder {
    internal_data_model: InternalDataModelRef,
}

impl QuerySchemaBuilder {
    pub fn new(internal_data_model: &InternalDataModelRef) -> QuerySchemaBuilder {
        QuerySchemaBuilder {
            internal_data_model: Arc::clone(internal_data_model),
        }
    }

    pub fn build(&self) -> QuerySchema {
        QuerySchema {
            query: self.build_query_type(),
            mutation: self.build_mutation_type(),
        }
    }

    /// Builds the root query type.
    fn build_query_type(&self) -> ObjectType {
        let non_embedded_models: Vec<&ModelRef> = self
            .internal_data_model
            .models()
            .iter()
            .filter(|m| !m.is_embedded)
            .collect();

        let fields: Vec<Field> = non_embedded_models
            .into_iter()
            .filter_map(|m| {
                Some(vec![
                    Self::all_items_field(Arc::clone(m)),
                    Self::single_item_field(Arc::clone(m)),
                ])
            })
            .flatten()
            .collect();

        object_type("Query", fields)
    }

    /// Builds the root mutation type.
    fn build_mutation_type(&self) -> ObjectType {
        object_type("Mutation", vec![])
    }

    /// Builds a "many" items field (e.g. users(args), posts(args), ...) for given model.
    fn all_items_field(model: ModelRef) -> Field {
        unimplemented!()
    }

    /// Builds a "single" item field (e.g. user(args), post(args), ...) for given model.
    fn single_item_field(model: ModelRef) -> Field {
        unimplemented!()
    }
}
