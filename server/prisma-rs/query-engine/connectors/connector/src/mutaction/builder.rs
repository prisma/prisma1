        /*
impl MutationBuilder {
    pub fn create_node(model: ModelRef, args: PrismaArgs) -> Insert {
        let model_id = cn.model.fields().id();
        let auto_generated_id = model_id.is_auto_generated_by_db;

        if !auto_generated_id {
            args.insert(&model_id.name, cn.model.generate_id());
        }

        let fields = cn
            .model
            .fields()
            .all
            .iter()
            .filter(|field| args.has_arg_for(&field.name))
            .collect();

        fields
            .iter()
            .fold(Insert::into(model.table), |query, (field_name, value)| {
                query.column(field.as_table());
                query.value(args.get_field_value(&field.name));
            })
            .returning(model_id.as_column())
        unimplemented!()
    }
}
         */
