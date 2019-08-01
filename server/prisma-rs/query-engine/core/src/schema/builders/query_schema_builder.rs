use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};
use std::sync::Arc;

/// Build mode for schema generation.
#[derive(Debug, Copy, Clone)]
pub enum BuildMode {
    /// Prisma 1 compatible schema generation.
    /// This will still generate only a subset of the legacy schema.
    Legacy,

    /// Prisma 2 schema. Uses different inflection strategy
    Modern,
}

/// Query schema builder. Root for query schema building.
///
/// The schema builder creates all builders necessary for the process,
/// and hands down references to the individual initializers as required.
///
/// Circular dependency schema building requires special consideration.
/// Assume a data model looks like this, with arrows indicating some kind of relation between models:
///
/// ```text
///       +---+
///   +---+ B +<---+
///   |   +---+    |
///   v            |
/// +-+-+        +-+-+      +---+
/// | A +------->+ C +<-----+ D |
/// +---+        +---+      +---+
/// ```
///
/// The above would cause infinite builder recursion circular
/// dependency (A -> B -> C -> A) in relations (for example in filter building).
///
/// Without caching, processing D (in fact, visiting any type after the intial computation) would also
/// trigger a complete recomputation of A, B, C.
///
/// Hence, all builders that produce input or output object types are required to
/// implement CachedBuilder in some form to break recursive type building.
///
/// Additionally, the cache also acts as the component to prevent memory leaks from circular dependencies
/// in the query schema later on, as described on the QuerySchema type.
/// The cache can be consumed to produce a list of strong references to the individual input and output
/// object types, which are then moved to the query schema to keep weak references alive (see TypeRefCache for additional infos).
pub struct QuerySchemaBuilder<'a> {
    mode: BuildMode,
    internal_data_model: InternalDataModelRef,
    _capabilities: &'a SupportedCapabilities,
    object_type_builder: Arc<ObjectTypeBuilder<'a>>,
    input_type_builder: Arc<InputTypeBuilder<'a>>,
    argument_builder: ArgumentBuilder<'a>,
    filter_object_type_builder: Arc<FilterObjectTypeBuilder<'a>>,
}

impl<'a> QuerySchemaBuilder<'a> {
    pub fn new(
        internal_data_model: &InternalDataModelRef,
        capabilities: &'a SupportedCapabilities,
        mode: BuildMode,
    ) -> Self {
        let filter_object_type_builder = Arc::new(FilterObjectTypeBuilder::new(capabilities));
        let input_type_builder = Arc::new(InputTypeBuilder::new(
            Arc::clone(internal_data_model),
            Arc::downgrade(&filter_object_type_builder),
        ));

        let object_type_builder = Arc::new(ObjectTypeBuilder::new(
            Arc::clone(internal_data_model),
            true,
            capabilities,
            Arc::downgrade(&filter_object_type_builder),
        ));

        let argument_builder = ArgumentBuilder::new(
            Arc::downgrade(&input_type_builder),
            Arc::downgrade(&object_type_builder),
        );

        QuerySchemaBuilder {
            mode,
            internal_data_model: Arc::clone(internal_data_model),
            _capabilities: capabilities,
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
        let output_objects = Arc::try_unwrap(self.object_type_builder).unwrap().into_strong_refs();
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
        let internal_data_model = Arc::clone(&self.internal_data_model);
        let (query_type, query_object_ref) = self.build_query_type();
        let (mutation_type, mutation_object_ref) = self.build_mutation_type();
        let (input_objects, mut output_objects) = self.collect_types();

        output_objects.push(query_object_ref);
        output_objects.push(mutation_object_ref);

        let query_type = Arc::new(query_type);
        let mutation_type = Arc::new(mutation_type);

        QuerySchema::new(
            query_type,
            mutation_type,
            input_objects,
            output_objects,
            internal_data_model,
        )
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

        let strong_ref = Arc::new(object_type("Query", fields, None));

        (OutputType::Object(Arc::downgrade(&strong_ref)), strong_ref)
    }

    /// Builds the root mutation type.
    fn build_mutation_type(&self) -> (OutputType, ObjectTypeStrongRef) {
        let non_embedded_models = self.non_embedded_models();
        let fields = non_embedded_models
            .into_iter()
            .map(|model| {
                let mut vec = vec![self.create_item_field(Arc::clone(&model))];

                append_opt(&mut vec, self.delete_item_field(Arc::clone(&model)));
                append_opt(&mut vec, self.update_item_field(Arc::clone(&model)));
                append_opt(&mut vec, self.upsert_item_field(Arc::clone(&model)));

                vec.push(self.update_many_field(Arc::clone(&model)));
                vec.push(self.delete_many_field(Arc::clone(&model)));

                vec
            })
            .flatten()
            .collect();

        let strong_ref = Arc::new(object_type("Mutation", fields, None));

        (OutputType::Object(Arc::downgrade(&strong_ref)), strong_ref)
    }

    /// Helper function to get all non-embedded models from the internal data model.
    fn non_embedded_models(&self) -> Vec<ModelRef> {
        self.internal_data_model
            .models()
            .iter()
            .filter(|m| !m.is_embedded)
            .map(|m| Arc::clone(m))
            .collect()
    }

    /// Builds a "single" query arity item field (e.g. "user", "post" ...) for given model.
    fn single_item_field(&self, model: ModelRef) -> Option<Field> {
        self.argument_builder
            .where_unique_argument(Arc::clone(&model))
            .map(|arg| {
                let field_name =
                    self.pluralize_internal(camel_case(model.name.clone()), format!("findOne{}", model.name.clone()));

                field(
                    field_name,
                    vec![arg],
                    OutputType::opt(OutputType::object(
                        self.object_type_builder.map_model_object_type(&model),
                    )),
                    Some(ModelOperation::new(Arc::clone(&model), OperationTag::FindOne)),
                )
            })
    }

    /// Builds a "multiple" query arity items field (e.g. "users", "posts", ...) for given model.
    fn all_items_field(&self, model: ModelRef) -> Field {
        let args = self.object_type_builder.many_records_arguments(&model);
        let field_name = self.pluralize_internal(
            camel_case(pluralize(model.name.clone())),
            format!("findMany{}", model.name.clone()),
        );

        field(
            field_name,
            args,
            OutputType::list(OutputType::opt(OutputType::object(
                self.object_type_builder.map_model_object_type(&model),
            ))),
            Some(ModelOperation::new(Arc::clone(&model), OperationTag::FindMany)),
        )
    }

    /// Builds a create mutation field (e.g. createUser) for given model.
    fn create_item_field(&self, model: ModelRef) -> Field {
        let args = self
            .argument_builder
            .create_arguments(Arc::clone(&model))
            .unwrap_or_else(|| vec![]);

        let field_name = self.pluralize_internal(
            format!("create{}", model.name),
            format!("createOne{}", model.name.clone()),
        );

        field(
            field_name,
            args,
            OutputType::object(self.object_type_builder.map_model_object_type(&model)),
            Some(ModelOperation::new(
                Arc::clone(&model),
                OperationTag::CreateOne(Box::new(OperationTag::FindOne)),
            )),
        )
    }

    /// Builds a delete mutation field (e.g. deleteUser) for given model.
    fn delete_item_field(&self, model: ModelRef) -> Option<Field> {
        self.argument_builder.delete_arguments(Arc::clone(&model)).map(|args| {
            let field_name = self.pluralize_internal(
                format!("delete{}", model.name),
                format!("deleteOne{}", model.name.clone()),
            );

            field(
                field_name,
                args,
                OutputType::opt(OutputType::object(
                    self.object_type_builder.map_model_object_type(&model),
                )),
                Some(ModelOperation::new(
                    Arc::clone(&model),
                    OperationTag::DeleteOne(Box::new(OperationTag::CoerceResultToOutputType)),
                )),
            )
        })
    }

    /// Builds a delete many mutation field (e.g. deleteManyUsers) for given model.
    fn delete_many_field(&self, model: ModelRef) -> Field {
        let arguments = self.argument_builder.delete_many_arguments(Arc::clone(&model));
        let field_name = self.pluralize_internal(
            format!("deleteMany{}", pluralize(model.name.clone())),
            format!("deleteMany{}", model.name.clone()),
        );

        field(
            field_name,
            arguments,
            OutputType::object(self.object_type_builder.batch_payload_object_type()),
            Some(ModelOperation::new(
                Arc::clone(&model),
                OperationTag::DeleteMany(Box::new(OperationTag::CoerceResultToOutputType)),
            )),
        )
    }

    /// Builds an update mutation field (e.g. updateUser) for given model.
    fn update_item_field(&self, model: ModelRef) -> Option<Field> {
        self.argument_builder.update_arguments(Arc::clone(&model)).map(|args| {
            let field_name =
                self.pluralize_internal(format!("update{}", model.name), format!("updateOne{}", model.name));

            field(
                field_name,
                args,
                OutputType::opt(OutputType::object(
                    self.object_type_builder.map_model_object_type(&model),
                )),
                Some(ModelOperation::new(
                    Arc::clone(&model),
                    OperationTag::UpdateOne(Box::new(OperationTag::FindOne)),
                )),
            )
        })
    }

    /// Builds an update many mutation field (e.g. updateManyUsers) for given model.
    fn update_many_field(&self, model: ModelRef) -> Field {
        let arguments = self.argument_builder.update_many_arguments(Arc::clone(&model));
        let field_name = self.pluralize_internal(
            format!("updateMany{}", pluralize(model.name.clone())),
            format!("updateMany{}", model.name.clone()),
        );

        field(
            field_name,
            arguments,
            OutputType::object(self.object_type_builder.batch_payload_object_type()),
            Some(ModelOperation::new(
                Arc::clone(&model),
                OperationTag::UpdateMany(Box::new(OperationTag::CoerceResultToOutputType)),
            )),
        )
    }

    /// Builds an upsert mutation field (e.g. upsertUser) for given model.
    fn upsert_item_field(&self, model: ModelRef) -> Option<Field> {
        self.argument_builder.upsert_arguments(Arc::clone(&model)).map(|args| {
            let field_name =
                self.pluralize_internal(format!("upsert{}", model.name), format!("upsertOne{}", model.name));

            field(
                field_name,
                args,
                OutputType::object(self.object_type_builder.map_model_object_type(&model)),
                Some(ModelOperation::new(
                    Arc::clone(&model),
                    OperationTag::UpsertOne(Box::new(OperationTag::FindOne)),
                )),
            )
        })
    }

    fn pluralize_internal(&self, legacy: String, modern: String) -> String {
        match self.mode {
            BuildMode::Legacy => legacy,
            BuildMode::Modern => modern,
        }
    }
}
