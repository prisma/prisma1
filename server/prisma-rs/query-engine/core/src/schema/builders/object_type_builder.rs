use super::*;
use prisma_models::{InternalDataModelRef, ModelRef, SortOrder};
use std::sync::Arc;

pub struct ObjectTypeBuilder<'a> {
  internal_data_model: InternalDataModelRef,
  with_relations: bool,
  only_id: bool,
  capabilities: &'a SupportedCapabilities,
}

impl<'a> ObjectTypeBuilder<'a> {
  pub fn model_object_type(model: ModelRef) -> ObjectType {
    // new ObjectType(
    //     name = model.name,
    //     description = None,
    //     fieldsFn = () => {
    //       model.fields
    //         .filter(_.isVisible)
    //         .filter(field =>
    //           field.isScalar match {
    //             case true  => true
    //             case false => withRelations
    //         })
    //         .map(mapClientField(model))
    //     },
    //     interfaces = {
    //       val idFieldHasRightType = model.idField.exists(f =>
    //         f.name == ReservedFields.idFieldName && (f.typeIdentifier == TypeIdentifier.String || f.typeIdentifier == TypeIdentifier.Cuid))
    //       if (model.hasVisibleIdField && idFieldHasRightType) nodeInterface.toList else List.empty
    //     },
    //     instanceCheck = (value: Any, valClass: Class[_], tpe: ObjectType[ApiUserContext, _]) =>
    //       value match {
    //         case PrismaNode(_, _, Some(tpe.name)) => true
    //         case PrismaNode(_, _, Some(_))        => false
    //         case _                                => valClass.isAssignableFrom(value.getClass)
    //     },
    //     astDirectives = Vector.empty
    //   )

    ObjectType {
      name: model.name.clone(),
      fields: vec![],
    };

    unimplemented!()
  }

  /// Builds "many records where" arguments based on the given model.
  pub fn many_records_arguments(model: ModelRef) -> Vec<Argument> {
    vec![
      // where_argument(&model, capabilities),
      Self::order_by_argument(&model),
      argument("skip", InputType::opt(InputType::string())),
      argument("after", InputType::opt(InputType::string())),
      argument("before", InputType::opt(InputType::string())),
      argument("first", InputType::opt(InputType::int())),
      argument("last", InputType::opt(InputType::int())),
    ]
  }

  pub fn where_argument(model: &ModelRef, capabilities: &SupportedCapabilities) -> Argument {
    let where_object = FilterObjectTypeBuilder {
      model: Arc::clone(model),
    }
    .build(capabilities);

    argument("where", InputType::opt(where_object))
  }

  pub fn order_by_argument(model: &ModelRef) -> Argument {
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
}
