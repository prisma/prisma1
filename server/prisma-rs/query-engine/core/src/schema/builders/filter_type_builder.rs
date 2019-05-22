use super::*;
use prisma_models::{Field as ModelField, ModelRef, RelationField, ScalarField, TypeIdentifier};
use std::sync::Arc;

pub struct FilterObjectTypeBuilder<'a> {
  pub model: ModelRef,
  pub capabilities: &'a SupportedCapabilities,
}

impl<'a> FilterObjectTypeBuilder<'a> {
  pub fn new(model: ModelRef, capabilities: &'a SupportedCapabilities) -> Self {
    FilterObjectTypeBuilder { model, capabilities }
  }

  pub fn build(&self) -> InputObjectTypeRef {
    if self.capabilities.has(ConnectorCapability::MongoJoinRelationLinks) {
      self.mongo_filter_object()
    } else {
      self.filter_object()
    }
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
  /// This code recurses into new FilterObjectTypeBuilders to build dependent filter types.
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
  /// The above would cause infinite filter type builder to be instantiated due to the circular dependency (A -> B -> C -> A) in relations.
  /// To break circles, all parents of the current recursion path are passed into the new FilterObjectTypeBuilder,
  /// e.g. for C, B and A InputObjectTypes are passed.
  ///
  /// As soon as a related model is already in the list of "parents", we know that we can break and reuse the already computed type.
  /// This will bubble up, allowing the field computation to finish.
  ///
  /// Model D in the graph illustrates that in the current solution, we would recompute
  fn map_relation_filter_input_field(&self, field: Arc<RelationField>) -> Vec<InputField> {
    let related_input_type = Self::new(Arc::clone(&field.related_model()), self.capabilities).build();

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

  fn filter_object(&self) -> InputObjectTypeRef {
    let input_object = Arc::new(init_input_object_type(format!("{}WhereInput", self.model.name.clone())));

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

    let mut scalar_input_fields: Vec<InputField> = self
      .model
      .fields()
      .scalar()
      .into_iter()
      .filter(|sf| !sf.is_hidden)
      .map(|sf| self.map_input_field(sf))
      .flatten()
      .collect();

    let mut relational_input_fields: Vec<InputField> = self
      .model
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

  fn mongo_filter_object(&self) -> InputObjectTypeRef {
    unimplemented!()
  }
}
