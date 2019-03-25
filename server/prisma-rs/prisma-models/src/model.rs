use crate::prelude::*;
use once_cell::sync::OnceCell;
use prisma_query::ast::{Column, Table};
use rand::{self, Rng};
use std::{
    sync::{Arc, Weak},
    time::{SystemTime, UNIX_EPOCH},
};
use uuid::{v1::Context, Uuid};

pub type ModelRef = Arc<Model>;
pub type ModelWeakRef = Weak<Model>;

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTemplate {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<FieldTemplate>,
    pub manifestation: Option<ModelManifestation>,
}

#[derive(DebugStub)]
pub struct Model {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub manifestation: Option<ModelManifestation>,

    fields: OnceCell<Fields>,
    id_context: Context, // TODO: replace uuid with cuid!

    #[debug_stub = "#SchemaWeakRef#"]
    pub schema: SchemaWeakRef,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelManifestation {
    pub db_name: String,
}

impl ModelTemplate {
    pub fn build(self, schema: SchemaWeakRef) -> ModelRef {
        let model = Arc::new(Model {
            name: self.name,
            stable_identifier: self.stable_identifier,
            is_embedded: self.is_embedded,
            fields: OnceCell::new(),
            manifestation: self.manifestation,
            id_context: Context::new(65535), // TODO: replace uuid with cuid!
            schema: schema,
        });

        let fields = Fields::new(
            self.fields
                .into_iter()
                .map(|fi| fi.build(Arc::downgrade(&model)))
                .collect(),
            Arc::downgrade(&model),
        );

        // The model is created here and fields WILL BE UNSET before now!
        model.fields.set(fields).unwrap();

        model
    }
}

impl Model {
    pub fn generate_id(&self) -> GraphqlId {
        match self.fields().id().type_identifier {
            TypeIdentifier::GraphQLID => {
                let start = SystemTime::now();
                let since_the_epoch = start.duration_since(UNIX_EPOCH).unwrap(); // will panic if time went backwards
                let mut rng = rand::thread_rng();

                let mut node_id = [0u8; 6];
                rng.fill(&mut node_id);

                let uuid = Uuid::new_v1(
                    &self.id_context,
                    since_the_epoch.as_secs(),
                    since_the_epoch.subsec_nanos(),
                    &node_id,
                )
                .unwrap(); // will panic if node_id is not six u8's

                GraphqlId::String(uuid.to_hyphenated().to_string())
            }
            TypeIdentifier::UUID => GraphqlId::UUID(Uuid::new_v4()),
            TypeIdentifier::Int => panic!("Cannot generate integer ids."),
            t => panic!("You shouldn't even use ids of type {:?}", t),
        }
    }

    pub fn table(&self) -> Table {
        (self.schema().db_name.as_str(), self.db_name()).into()
    }

    pub fn fields(&self) -> &Fields {
        self.fields
            .get()
            .ok_or_else(|| String::from("Model fields must be set!"))
            .unwrap()
    }

    pub fn is_legacy(&self) -> bool {
        self.schema().is_legacy()
    }

    pub fn db_name(&self) -> &str {
        self.db_name_opt().unwrap_or_else(|| self.name.as_ref())
    }

    pub fn db_name_opt(&self) -> Option<&str> {
        self.manifestation.as_ref().map(|mf| mf.db_name.as_ref())
    }

    pub fn schema(&self) -> SchemaRef {
        self.schema
            .upgrade()
            .expect("Schema does not exist anymore. Parent schema is deleted without deleting the child schema.")
    }

    pub fn id_column(&self) -> Column {
        self.fields().id().as_column()
    }
}
