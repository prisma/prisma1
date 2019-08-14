use super::*;
use prisma_models::{Field as ModelField, ModelRef, RelationFieldRef, ScalarFieldRef};
use std::sync::Arc;

/// Filter object and scalar filter object type builder.
#[derive(Debug)]
pub struct FilterObjectTypeBuilder<'a> {
    capabilities: &'a SupportedCapabilities,
    input_object_cache: TypeRefCache<InputObjectType>, // Caches "xWhereInput" / "xWhereScalarInput" -> Object type ref
}

impl<'a> InputBuilderExtensions for FilterObjectTypeBuilder<'a> {}

impl<'a> CachedBuilder<InputObjectType> for FilterObjectTypeBuilder<'a> {
    fn get_cache(&self) -> &TypeRefCache<InputObjectType> {
        &self.input_object_cache
    }

    fn into_strong_refs(self) -> Vec<Arc<InputObjectType>> {
        self.input_object_cache.into()
    }
}

impl<'a> FilterObjectTypeBuilder<'a> {
    pub fn new(capabilities: &'a SupportedCapabilities) -> Self {
        FilterObjectTypeBuilder {
            capabilities,
            input_object_cache: TypeRefCache::new(),
        }
    }

    pub fn scalar_filter_object_type(&self, model: ModelRef) -> InputObjectTypeRef {
        let object_name = format!("{}ScalarWhereInput", model.name);
        return_cached!(self.get_cache(), &object_name);

        let input_object = Arc::new(init_input_object_type(object_name.clone()));
        self.cache(object_name, Arc::clone(&input_object));

        let weak_ref = Arc::downgrade(&input_object);
        let mut input_fields = vec![
            input_field(
                "AND",
                InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
                None,
            ),
            input_field(
                "OR",
                InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
                None,
            ),
            input_field(
                "NOT",
                InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
                None,
            ),
        ];

        let fields: Vec<ScalarFieldRef> = model.fields().scalar().into_iter().filter(|f| !f.is_hidden).collect();
        let mut fields: Vec<InputField> = fields.into_iter().flat_map(|f| self.map_input_field(f)).collect();

        input_fields.append(&mut fields);
        input_object.set_fields(input_fields);

        weak_ref
    }

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
                None,
            ),
            input_field(
                "OR",
                InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
                None,
            ),
            input_field(
                "NOT",
                InputType::opt(InputType::list(InputType::object(Weak::clone(&weak_ref)))),
                None,
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

    fn build_mongo_filter_object(&self, _model: ModelRef) -> InputObjectTypeRef {
        unimplemented!()
    }

    fn map_input_field(&self, field: ScalarFieldRef) -> Vec<InputField> {
        get_field_filters(&ModelField::Scalar(Arc::clone(&field))) // wip: take a look at required signatures
            .into_iter()
            .map(|arg| {
                let field_name = format!("{}{}", field.name, arg.suffix);
                let mapped = self.map_required_input_type(Arc::clone(&field));

                if arg.is_list {
                    input_field(field_name, InputType::opt(InputType::list(mapped)), None)
                } else {
                    input_field(field_name, InputType::opt(mapped), None)
                }
            })
            .collect()
    }

    /// Maps relations to (filter) input fields.
    fn map_relation_filter_input_field(&self, field: RelationFieldRef) -> Vec<InputField> {
        let related_model = field.related_model();
        let related_input_type = self.filter_object_type(related_model);

        match (field.is_hidden, field.is_list) {
            (true, _) => vec![],
            (_, false) => vec![input_field(
                field.name.clone(),
                InputType::opt(InputType::object(Weak::clone(&related_input_type))),
                None,
            )],
            (_, true) => get_field_filters(&ModelField::Relation(Arc::clone(&field)))
                .into_iter()
                .map(|arg| {
                    let field_name = format!("{}{}", field.name, arg.suffix);
                    let typ = InputType::opt(InputType::object(Weak::clone(&related_input_type)));
                    input_field(field_name, typ, None)
                })
                .collect(),
        }
    }
}
