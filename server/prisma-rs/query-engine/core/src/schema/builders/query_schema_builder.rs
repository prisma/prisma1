use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};
use std::sync::Arc;

/// WIP. Build mode for schema generation.
#[derive(Debug, Copy, Clone)]
pub enum BuildMode {
  /// Prisma 1 compatible schema generation.
  /// This will still generate only a subset of the legacy schema.
  Legacy,

  /// Prisma 2 schema.
  Modern,
}

/// Query schema builder. Root for query schema building.
/// The schema builder creates all builders necessary for the process,
/// and hands down references to the individual initializers as required.
pub struct QuerySchemaBuilder<'a> {
  mode: BuildMode,
  internal_data_model: InternalDataModelRef,
  capabilities: &'a SupportedCapabilities,
  object_type_builder: ObjectTypeBuilder<'a>,
  input_type_builder: Arc<InputTypeBuilder>,
  argument_builder: ArgumentBuilder,
  filter_object_type_builder: Arc<FilterObjectTypeBuilder<'a>>,
}

impl<'a> QuerySchemaBuilder<'a> {
  pub fn new(
    internal_data_model: &InternalDataModelRef,
    capabilities: &'a SupportedCapabilities,
    mode: BuildMode,
  ) -> Self {
    let input_type_builder = Arc::new(InputTypeBuilder::new(Arc::clone(internal_data_model)));
    let argument_builder = ArgumentBuilder::new(Arc::clone(internal_data_model), Arc::downgrade(&input_type_builder));
    let filter_object_type_builder = Arc::new(FilterObjectTypeBuilder::new(capabilities));
    let object_type_builder = ObjectTypeBuilder::new(
      Arc::clone(internal_data_model),
      true,
      capabilities,
      Arc::downgrade(&filter_object_type_builder),
    );

    QuerySchemaBuilder {
      mode,
      internal_data_model: Arc::clone(internal_data_model),
      capabilities,
      object_type_builder,
      input_type_builder,
      argument_builder,
      filter_object_type_builder,
    }
  }

  /// Consumes the builders and collects all types from all builder caches to merge
  /// them into the vectors required to finalize the query schema building.
  /// Unwraps are safe because only the query schema builder holds the strong ref,
  /// which makes the Arc counter 1, all other refs are weak refs.
  fn collect_types(self) -> (Vec<InputObjectTypeStrongRef>, Vec<ObjectTypeStrongRef>) {
    let output_objects = self.object_type_builder.into_strong_refs();
    let mut input_objects = Arc::try_unwrap(self.input_type_builder).unwrap().into_strong_refs();
    let mut filter_objects = Arc::try_unwrap(self.filter_object_type_builder)
      .unwrap()
      .into_strong_refs();

    input_objects.append(&mut filter_objects);
    (input_objects, output_objects)
  }

  /// TODO filter empty input types
  /// Consumes the builder to create the query schema.
  pub fn build(self) -> QuerySchema {
    let (query_type, query_object_ref) = self.build_query_type();
    let (mutation_type, mutation_object_ref) = self.build_mutation_type();
    let (input_objects, mut output_objects) = self.collect_types();

    output_objects.push(query_object_ref);
    output_objects.push(mutation_object_ref);

    QuerySchema::new(query_type, mutation_type, input_objects, output_objects)
  }

  /// Builds the root query type.
  fn build_query_type(&self) -> (OutputType, ObjectTypeStrongRef) {
    let non_embedded_models = self.non_embedded_models();
    let fields = non_embedded_models
      .into_iter()
      .map(|m| {
        let mut vec = vec![self.all_items_field(Arc::clone(&m))];
        append_opt(&mut vec, self.single_item_field(Arc::clone(&m)));

        vec
      })
      .flatten()
      .collect();

    let strong_ref = Arc::new(object_type("Query", fields));

    (OutputType::Object(Arc::downgrade(&strong_ref)), strong_ref)
  }

  /// Builds the root mutation type.
  fn build_mutation_type(&self) -> (OutputType, ObjectTypeStrongRef) {
    let non_embedded_models = self.non_embedded_models();
    let fields = non_embedded_models
      .into_iter()
      .map(|m| {
        let vec = vec![self.create_item_field(Arc::clone(&m))];
        vec
      })
      .flatten()
      .collect();

    let strong_ref = Arc::new(object_type("Mutation", fields));

    (OutputType::Object(Arc::downgrade(&strong_ref)), strong_ref)
  }

  fn non_embedded_models(&self) -> Vec<ModelRef> {
    self
      .internal_data_model
      .models()
      .iter()
      .filter(|m| !m.is_embedded)
      .map(|m| Arc::clone(m))
      .collect()
  }

  /// Builds a "multiple" query arity items field (e.g. "users", "posts", ...) for given model.
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

  /// Builds a "single" query arity item field (e.g. "user", "post" ...) for given model.
  fn single_item_field(&self, model: ModelRef) -> Option<Field> {
    self
      .argument_builder
      .where_unique_argument(Arc::clone(&model))
      .map(|arg| {
        field(
          camel_case(model.name.clone()),
          vec![arg],
          OutputType::opt(OutputType::object(
            self.object_type_builder.map_model_object_type(&model),
          )),
        )
      })
  }

  /// Builds a create mutation field (e.g. createUser) for given model.
  fn create_item_field(&self, model: ModelRef) -> Field {
    let args = self
      .argument_builder
      .create_arguments(Arc::clone(&model))
      .unwrap_or_else(|| vec![]);

    field(
      format!("create{}", model.name),
      args,
      OutputType::object(self.object_type_builder.map_model_object_type(&model)),
    )
  }

  fn update_item_field(&self, model: ModelRef) -> Field {
    unimplemented!()
  }
}
