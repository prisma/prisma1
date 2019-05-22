use super::*;
use once_cell::sync::OnceCell;
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, ScalarField, SortOrder, TypeIdentifier};
use std::{collections::HashMap, sync::Arc};

type ObjectTypeCache = HashMap<String, ObjectTypeRef>;

pub struct ObjectTypeBuilder<'a> {
  internal_data_model: InternalDataModelRef,
  with_relations: bool,
  capabilities: &'a SupportedCapabilities,
  model_object_type_cache: OnceCell<ObjectTypeCache>, // Deduplicates computation
}

impl<'a> ObjectTypeBuilder<'a> {
  pub fn new(
    internal_data_model: InternalDataModelRef,
    with_relations: bool,
    capabilities: &'a SupportedCapabilities,
  ) -> Self {
    ObjectTypeBuilder {
      internal_data_model,
      with_relations,
      capabilities,
      model_object_type_cache: OnceCell::new(),
    }
    .compute_model_object_types()
  }

  pub fn map_model_object_type(&self, model: &ModelRef) -> ObjectTypeRef {
    Arc::clone(
      self
        .cache()
        .get(&model.name)
        .expect("Invariant violation: Initialized object type skeleton for each model."),
    )
  }

  /// Initializes model object type cache on the query schema builder.
  fn compute_model_object_types(self) -> Self {
    // Compute initial cache.
    self.model_object_type_cache.get_or_init(|| {
      self
        .internal_data_model
        .models()
        .iter()
        .map(|m| (m.name.clone(), Arc::new(init_object_type(m.name.clone()))))
        .collect()
    });

    // Compute fields on all cached object types.
    self.internal_data_model.models().iter().for_each(|m| {
      let obj: ObjectTypeRef = self.map_model_object_type(m);
      let fields = self.compute_fields(m);

      obj.fields.set(fields).unwrap();
    });

    self
  }

  /// This only initializes the model object type, but does not compute fields due to dependencies on other object types.
  /// Make sure to compute fields as soon as all model object types are available.
  // fn init_model_object_type(&self, model: ModelRef) -> ObjectType {
  // WIP: What about instance checks?
  //     instanceCheck = (value: Any, valClass: Class[_], tpe: ObjectType[ApiUserContext, _]) =>
  //       value match {
  //         case PrismaNode(_, _, Some(tpe.name)) => true
  //         case PrismaNode(_, _, Some(_))        => false
  //         case _                                => valClass.isAssignableFrom(value.getClass)
  //     },
  //     astDirectives = Vector.empty

  //   object_type(model.name.clone(), fields_fn)
  // }

  /// This assumes that the cache has already been initialized.
  fn compute_fields(&self, model: &ModelRef) -> Vec<Field> {
    model
      .fields()
      .all
      .iter()
      .filter(|f| {
        f.is_visible()
          && match f {
            ModelField::Scalar(_) => true,
            ModelField::Relation(_) => self.with_relations,
          }
      })
      .map(|f| self.map_field(model, f))
      .collect()
  }

  pub fn map_field(&self, model: &ModelRef, model_field: &ModelField) -> Field {
    field(
      model_field.name().clone(),
      self.many_records_field_arguments(&model, &model_field),
      self.map_output_type(&model_field),
    )
  }

  fn map_output_type(&self, model_field: &ModelField) -> OutputType {
    let output_type = match model_field {
      ModelField::Relation(rf) => {
        let related_model_obj = OutputType::object(self.map_model_object_type(&rf.related_model()));

        if rf.is_list {
          OutputType::list(related_model_obj)
        } else {
          related_model_obj
        }
      }
      ModelField::Scalar(sf) => match sf.type_identifier {
        TypeIdentifier::String => OutputType::string(),
        TypeIdentifier::Float => OutputType::float(),
        TypeIdentifier::Boolean => OutputType::boolean(),
        TypeIdentifier::Enum => Self::map_enum_field(sf).into(),
        TypeIdentifier::Json => OutputType::json(),
        TypeIdentifier::DateTime => OutputType::date_time(),
        TypeIdentifier::GraphQLID => OutputType::id(),
        TypeIdentifier::UUID => OutputType::uuid(),
        TypeIdentifier::Int => OutputType::int(),
        TypeIdentifier::Relation => unreachable!(), // Scalar fields can't have a Relation type identifier.
      },
    };

    if model_field.is_scalar() && model_field.is_list() {
      OutputType::list(output_type)
    } else if !model_field.is_required() {
      OutputType::opt(output_type)
    } else {
      output_type
    }
  }

  /// Builds "many records where" arguments based on the given model and field.
  pub fn many_records_field_arguments(&self, model: &ModelRef, field: &ModelField) -> Vec<Argument> {
    match field {
      f if !f.is_visible() => vec![],
      ModelField::Scalar(_) => vec![],
      ModelField::Relation(rf) if rf.is_list && !rf.related_model().is_embedded => self.many_records_arguments(model),
      ModelField::Relation(rf) if rf.is_list && rf.related_model().is_embedded => vec![],
      ModelField::Relation(rf) if rf.is_list => vec![],
      _ => unreachable!(),
    }
  }

  /// Builds "many records where" arguments solely based on the given model.
  pub fn many_records_arguments(&self, model: &ModelRef) -> Vec<Argument> {
    vec![
      self.where_argument(&model),
      self.order_by_argument(&model),
      argument("skip", InputType::opt(InputType::string())),
      argument("after", InputType::opt(InputType::string())),
      argument("before", InputType::opt(InputType::string())),
      argument("first", InputType::opt(InputType::int())),
      argument("last", InputType::opt(InputType::int())),
    ]
  }

  pub fn where_argument(&self, model: &ModelRef) -> Argument {
    let where_object = FilterObjectTypeBuilder::new(Arc::clone(model), self.capabilities).build();
    argument("where", InputType::opt(InputType::object(where_object)))
  }

  pub fn order_by_argument(&self, model: &ModelRef) -> Argument {
    let enum_values: Vec<EnumValue> = model
      .fields()
      .scalar_non_list()
      .iter()
      .map(|f| {
        vec![
          EnumValue::order_by(
            format!("{}_{}", f.name, SortOrder::Ascending.abbreviated()),
            Arc::clone(f),
            SortOrder::Ascending,
          ),
          EnumValue::order_by(
            format!("{}_{}", f.name, SortOrder::Descending.abbreviated()),
            Arc::clone(f),
            SortOrder::Descending,
          ),
        ]
      })
      .flatten()
      .collect();

    let enum_name = format!("{}OrderByInput", model.name);
    let enum_type = enum_type(enum_name, enum_values);

    argument("orderBy", InputType::opt(enum_type.into()))
  }

  pub fn map_enum_field(scalar_field: &Arc<ScalarField>) -> EnumType {
    match scalar_field.type_identifier {
      TypeIdentifier::Enum => {
        let internal_enum = scalar_field
          .internal_enum
          .as_ref()
          .expect("Invariant violation: Enum fields are expected to have an internal_enum associated with them.");

        internal_enum.into()
      }
      _ => panic!("Invariant violation: map_enum_field can only be called on scalar enum fields."),
    }
  }

  fn cache(&self) -> &ObjectTypeCache {
    &self
      .model_object_type_cache
      .get()
      .expect("Invariant violation: Expected cache to be initialized before computing fields.")
  }
}
