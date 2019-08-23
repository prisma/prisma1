use crate::prelude::*;
use once_cell::sync::OnceCell;
use prisma_query::ast::{Column, Table};
use std::sync::{Arc, Weak};
use uuid::Uuid;

pub type ModelRef = Arc<Model>;
pub type ModelWeakRef = Weak<Model>;

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTemplate {
    pub name: String,
    pub stable_identifier: String, // todo: remove once we fully switched to dm v2
    pub is_embedded: bool,
    pub fields: Vec<FieldTemplate>,
    pub manifestation: Option<ModelManifestation>, // todo: convert to Option<String> once we fully switched to dm v2
}

#[derive(DebugStub)]
pub struct Model {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub manifestation: Option<ModelManifestation>,

    fields: OnceCell<Fields>,

    #[debug_stub = "#InternalDataModelWeakRef#"]
    pub internal_data_model: InternalDataModelWeakRef,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelManifestation {
    pub db_name: String,
}

impl ModelTemplate {
    pub fn build(self, internal_data_model: InternalDataModelWeakRef) -> ModelRef {
        let model = Arc::new(Model {
            name: self.name,
            stable_identifier: self.stable_identifier,
            is_embedded: self.is_embedded,
            fields: OnceCell::new(),
            manifestation: self.manifestation,
            internal_data_model,
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

impl PartialEq for Model {
    fn eq(&self, other: &Model) -> bool {
        self.name == other.name
    }
}

impl Model {
    pub fn generate_id(&self) -> GraphqlId {
        match self.fields().id().type_identifier {
            // This will panic when:
            //
            // - System time goes backwards
            // - There is an error generating a fingerprint
            // - Time cannot be converted to a string.
            //
            // Panic is a better choice than bubbling this up
            TypeIdentifier::GraphQLID => GraphqlId::String(cuid::cuid().unwrap()),
            TypeIdentifier::UUID => GraphqlId::UUID(Uuid::new_v4()),
            TypeIdentifier::Int => panic!("Cannot generate integer ids."),
            t => panic!("You shouldn't even use ids of type {:?}", t),
        }
    }

    pub fn table(&self) -> Table<'static> {
        (self.internal_data_model().db_name.clone(), self.db_name().to_string()).into()
    }

    pub fn fields(&self) -> &Fields {
        self.fields
            .get()
            .ok_or_else(|| String::from("Model fields must be set!"))
            .unwrap()
    }

    pub fn is_legacy(&self) -> bool {
        self.internal_data_model().is_legacy()
    }

    pub fn db_name(&self) -> &str {
        self.db_name_opt().unwrap_or_else(|| self.name.as_ref())
    }

    pub fn db_name_opt(&self) -> Option<&str> {
        self.manifestation.as_ref().map(|mf| mf.db_name.as_ref())
    }

    pub fn internal_data_model(&self) -> InternalDataModelRef {
        self.internal_data_model
            .upgrade()
            .expect("InternalDataModel does not exist anymore. Parent internal_data_model is deleted without deleting the child internal_data_model.")
    }

    pub fn id_column(&self) -> Column<'static> {
        self.fields().id().as_column()
    }
}
