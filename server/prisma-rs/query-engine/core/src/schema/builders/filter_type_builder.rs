use super::*;
use prisma_models::{Field as ModelField, ModelRef, RelationField, ScalarField, TypeIdentifier};
use std::{cell::RefCell, collections::HashMap, sync::Arc};

/// Filter object and scalar filter object type builder.
/// Not thread safe.
/// RefCells are used to provide interior mutability to the cache without requiring mut refs to the builder.
pub struct FilterObjectTypeBuilder<'a> {
  capabilities: &'a SupportedCapabilities,
  filter_cache: RefCell<HashMap<String, InputObjectTypeRef>>, // Caches "xWhereInput": Model name -> Object type ref
  scalar_cache: RefCell<HashMap<String, InputObjectTypeRef>>, // Caches "xWhereScalarInput": Model name -> Object type ref
}

impl<'a> FilterObjectTypeBuilder<'a> {
  pub fn new(capabilities: &'a SupportedCapabilities) -> Self {
    FilterObjectTypeBuilder {
      capabilities,
      filter_cache: RefCell::new(HashMap::new()),
      scalar_cache: RefCell::new(HashMap::new()),
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
    let existing_entry = self.filter_cache.borrow().get(&model.name).cloned();

    match existing_entry {
      Some(entry) => entry,
      None => {
        let input_object = Arc::new(init_input_object_type(format!("{}WhereInput", model.name.clone())));
        self
          .filter_cache
          .borrow_mut()
          .insert(model.name.clone(), Arc::clone(&input_object));

        // WIP: the field init below might need to be deferred...
        let mut fields = vec![
          input_field(
            "AND",
            InputType::opt(InputType::list(InputType::object(Arc::clone(&input_object)))),
          ),
          input_field(
            "OR",
            InputType::opt(InputType::list(InputType::object(Arc::clone(&input_object)))),
          ),
          input_field(
            "NOT",
            InputType::opt(InputType::list(InputType::object(Arc::clone(&input_object)))),
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

        input_object.fields.set(fields).unwrap();
        input_object
      }
    }
  }

  fn build_mongo_filter_object(&self, model: ModelRef) -> InputObjectTypeRef {
    unimplemented!()
  }

  fn map_input_field(&self, field: Arc<ScalarField>) -> Vec<InputField> {
    get_field_filters(&ModelField::Scalar(Arc::clone(&field))) // wip: take a look at required signatures
      .into_iter()
      .map(|arg| {
        let field_name = format!("{}{}", field.name, arg.suffix);
        let mapped = self.map_required_input_type(Arc::clone(&field));

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

    // We need to separate the two variables to drop the RefCell borrow in between.
    let related_input_type_opt = self.filter_cache.borrow().get(&related_model.name).cloned();
    let related_input_type = related_input_type_opt.unwrap_or_else(|| self.filter_object_type(related_model));

    match (field.is_hidden, field.is_list) {
      (true, _) => vec![],
      (_, false) => vec![input_field(
        field.name.clone(),
        InputType::opt(InputType::object(Arc::clone(&related_input_type))),
      )],
      (_, true) => get_field_filters(&ModelField::Relation(Arc::clone(&field)))
        .into_iter()
        .map(|arg| {
          let field_name = format!("{}{}", field.name, arg.suffix);
          let typ = InputType::opt(InputType::object(Arc::clone(&related_input_type)));
          input_field(field_name, typ)
        })
        .collect(),
    }
  }

  fn map_required_input_type(&self, field: Arc<ScalarField>) -> InputType {
    let typ = match field.type_identifier {
      TypeIdentifier::String => InputType::string(),
      TypeIdentifier::Int => InputType::int(),
      TypeIdentifier::Float => InputType::float(),
      TypeIdentifier::Boolean => InputType::boolean(),
      TypeIdentifier::GraphQLID => InputType::id(),
      TypeIdentifier::UUID => InputType::uuid(),
      TypeIdentifier::DateTime => InputType::date_time(),
      TypeIdentifier::Json => InputType::json(),
      TypeIdentifier::Enum => self.map_enum_input_type(&field).into(),
      TypeIdentifier::Relation => unreachable!(), // A scalar field can't be a relation.
    };

    if field.is_list {
      InputType::list(typ)
    } else {
      typ
    }
  }

  fn map_enum_input_type(&self, field: &Arc<ScalarField>) -> InputType {
    let internal_enum = field
      .internal_enum
      .as_ref()
      .expect("A field with TypeIdentifier Enum must always have an enum.");

    let et: EnumType = internal_enum.into();
    et.into()
  }
}
