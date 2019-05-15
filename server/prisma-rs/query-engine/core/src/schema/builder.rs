use prisma_inflector;
use prisma_models::InternalDataModelRef;

pub struct SchemaBuilder;

impl SchemaBuilder {
    pub fn build(data_model: InternalDataModelRef) {
        data_model.models().into_iter().for_each(|m| {
            let candidate = prisma_inflector::default().pluralize(&m.name);
            println!("{} -> {:?}", &m.name, candidate);
        });
    }
}
