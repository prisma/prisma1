use prisma_inflector::{self, Pluralize};
use prisma_models::SchemaRef;

pub struct SchemaBuilder;

impl SchemaBuilder {
    pub fn build(data_model: SchemaRef) {
        data_model.models().into_iter().for_each(|m| {
            let candidate = prisma_inflector::default.pluralize(&m.name);
            println!("{} -> {:?}", &m.name, candidate);
        });
    }
}
