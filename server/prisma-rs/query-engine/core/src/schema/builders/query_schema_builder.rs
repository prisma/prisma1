use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};
use std::sync::Arc;

pub struct QuerySchemaBuilder<'a> {
  internal_data_model: InternalDataModelRef,
  capabilities: &'a SupportedCapabilities,
  object_type_builder: ObjectTypeBuilder<'a>,
}

impl<'a> QuerySchemaBuilder<'a> {
  pub fn new(internal_data_model: &InternalDataModelRef, capabilities: &'a SupportedCapabilities) -> Self {
    let object_type_builder = ObjectTypeBuilder::new(Arc::clone(internal_data_model), true, capabilities);

    QuerySchemaBuilder {
      internal_data_model: Arc::clone(internal_data_model),
      object_type_builder,
      capabilities,
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
    let non_embedded_models: Vec<ModelRef> = self
      .internal_data_model
      .models()
      .iter()
      .filter(|m| !m.is_embedded)
      .map(|m| Arc::clone(m))
      .collect();

    let fields = non_embedded_models
      .into_iter()
      .filter_map(|m| {
        Some(vec![
          self.all_items_field(Arc::clone(&m)),
          self.single_item_field(Arc::clone(&m)),
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
  fn all_items_field(&self, model: ModelRef) -> Field {
    let args = self.object_type_builder.many_records_arguments(&model);

    field(
      camel_case(pluralize(model.name.clone())),
      args,
      OutputType::list(OutputType::opt(OutputType::object(
        self.object_type_builder.map_model_object_type(&model),
      ))),
    )
  }

  /// Builds a "single" item field (e.g. user(args), post(args), ...) for given model.
  fn single_item_field(&self, model: ModelRef) -> Field {
    unimplemented!()
  }
}
