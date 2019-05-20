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
    let object_type_builder = ObjectTypeBuilder::new(Arc::clone(internal_data_model), true, false, capabilities);

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
          Self::all_items_field(Arc::clone(&m)),
          Self::single_item_field(Arc::clone(&m)),
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
    // Field {
    //     name: camel_case(pluralize(model.name)),
    //     arguments: vec![],
    //     field_type: OutputType::list(OutputType::opt(OutputType::Object))
    // }

    // Field(
    //     camelCase(pluralsCache.pluralName(model)),
    //     fieldType = ListType(OptionType(objectTypes(model.name))),
    //     arguments = objectTypeBuilder.mapToListConnectionArguments(model),
    //     resolve = ctx => {
    //         val arguments = objectTypeBuilder.extractQueryArgumentsFromContext(model, ctx)
    //         DeferredValue(GetNodesDeferred(model, arguments, ctx.getSelectedFields(model))).map(_.toNodes.map(Some(_)))
    //     }
    // )
    unimplemented!()
  }

  /// Builds a "single" item field (e.g. user(args), post(args), ...) for given model.
  fn single_item_field(model: ModelRef) -> Field {
    unimplemented!()
  }
}
