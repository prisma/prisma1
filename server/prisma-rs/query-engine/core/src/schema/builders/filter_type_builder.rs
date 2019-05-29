use super::*;
use prisma_models::{Field as ModelField, ModelRef, RelationField, ScalarField, TypeIdentifier};
use std::sync::Arc;

/// Filter object and scalar filter object type builder.
pub struct FilterObjectTypeBuilder<'a> {
  input_type_builder: Arc<InputTypeBuilder>,
  capabilities: &'a SupportedCapabilities,
  input_object_cache: TypeRefCache<InputObjectType>, // Caches "xWhereInput" / "xWhereScalarInput" -> Object type ref
}

impl<'a> CachedBuilder<InputObjectType> for FilterObjectTypeBuilder<'a> {
  fn get_cache(&self) -> &TypeRefCache<InputObjectType> {
    &self.input_object_cache
  }
}

impl<'a> FilterObjectTypeBuilder<'a> {
  pub fn new(input_type_builder: Arc<InputTypeBuilder>, capabilities: &'a SupportedCapabilities) -> Self {
    FilterObjectTypeBuilder {
      input_type_builder,
      capabilities,
      input_object_cache: TypeRefCache::new(),
    }
  }

  // todo: scalarFilterObjectType

  pub fn filter_object_type(&self, model: ModelRef) -> InputObjectTypeRef {
    if self.capabilities.has(ConnectorCapability::MongoJoinRelationLinks) {
      self.build_mongo_filter_object(model)
    } else {
      self.build_filter_object(model)
    }
  }

  fn build_filter_object(&self, model: ModelRef) -> InputObjectTypeRef {
    let name = format!("{}WhereInput", model.name.clone());
    return_cached!(self.input_object_cache, &name);

    let input_object = Arc::new(init_input_object_type(name.clone()));
    self.cache(name, Arc::clone(&input_object));

    let weak_ref = Arc::downgrade(&input_object);
    let mut fields = vec![
      input_field(
        "AND",
        InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
      ),
      input_field(
        "OR",
        InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
      ),
      input_field(
        "NOT",
        InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
      ),
    ];

    let mut scalar_input_fields: Vec<InputField> = model
      .fields()
      .scalar()
      .into_iter()
      .filter(|sf| !sf.is_hidden)
      .map(|sf| self.map_input_field(sf))
      .flatten()
      .collect();

    let mut relational_input_fields: Vec<InputField> = model
      .fields()
      .relation()
      .into_iter()
      .map(|rf| self.map_relation_filter_input_field(rf))
      .flatten()
      .collect();

    fields.append(&mut scalar_input_fields);
    fields.append(&mut relational_input_fields);

    input_object.set_fields(fields);
    weak_ref
  }

  fn build_mongo_filter_object(&self, model: ModelRef) -> InputObjectTypeRef {
    unimplemented!()
  }

  fn map_input_field(&self, field: Arc<ScalarField>) -> Vec<InputField> {
    get_field_filters(&ModelField::Scalar(Arc::clone(&field))) // wip: take a look at required signatures
      .into_iter()
      .map(|arg| {
        let field_name = format!("{}{}", field.name, arg.suffix);
        let mapped = self.input_type_builder.map_required_input_type(Arc::clone(&field));

        if arg.is_list {
          input_field(field_name, InputType::opt(InputType::list(mapped)))
        } else {
          input_field(field_name, InputType::opt(mapped))
        }
      })
      .collect()
  }

  /// Maps relations to input fields.
  ///
  /// This function also triggers building dependent filter object types if they're not already cached.
  ///
  /// This needs special consideration, due to circular dependencies.
  /// Assume a data model looks like this, with arrows indicating some kind of relation between models:
  ///
  ///       +---+
  ///   +---+ B +<---+
  ///   |   +---+    |
  ///   v            |
  /// +-+-+        +-+-+      +---+
  /// | A +------->+ C +<-----+ D |
  /// +---+        +---+      +---+
  ///
  /// The above would cause infinite filter type builder to be instantiated due to the circular
  /// dependency (A -> B -> C -> A) in relations without the cache to break circles.
  ///
  /// Without caching, processing D (in fact, any type) would also trigger a complete recomputation of A, B, C.
  fn map_relation_filter_input_field(&self, field: Arc<RelationField>) -> Vec<InputField> {
    let related_model = field.related_model();
    let related_input_type = self.filter_object_type(related_model);

    match (field.is_hidden, field.is_list) {
      (true, _) => vec![],
      (_, false) => vec![input_field(
        field.name.clone(),
        InputType::opt(InputType::object(Weak::clone(&related_input_type))),
      )],
      (_, true) => get_field_filters(&ModelField::Relation(Arc::clone(&field)))
        .into_iter()
        .map(|arg| {
          let field_name = format!("{}{}", field.name, arg.suffix);
          let typ = InputType::opt(InputType::object(Weak::clone(&related_input_type)));
          input_field(field_name, typ)
        })
        .collect(),
    }
  }
}
