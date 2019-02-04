use crate::project::Renameable;
use std::{cell::RefCell, rc::Rc};

static ID_FIELD: &str = "id";
static EMBEDDED_ID_FIELD: &str = "_id";
static UPDATED_AT_FIELD: &str = "updatedAt";
static CREATED_AT_FIELD: &str = "createdAt";

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SchemaTemplate {
    pub models: Vec<ModelTemplate>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
}

#[derive(Debug)]
pub struct Schema {
    pub models: Vec<Rc<RefCell<Model>>>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
    pub version: Option<String>,
}

impl Into<Rc<RefCell<Schema>>> for SchemaTemplate {
    fn into(self) -> Rc<RefCell<Schema>> {
        let schema = Rc::new(RefCell::new(Schema {
            models: Vec::new(),
            relations: self.relations,
            enums: self.enums,
            version: self.version,
        }));

        self.models
            .into_iter()
            .for_each(|mt| schema.borrow_mut().models.push(mt.build(schema.clone())));

        schema
    }
}

impl Schema {
    pub fn find_model(&self, name: &str) -> Option<Rc<RefCell<Model>>> {
        self.models
            .iter()
            .find(|model| model.borrow().name == name)
            .map(|model| model.clone())
    }

    pub fn is_legacy(&self) -> bool {
        self.version.is_none()
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OnDelete {
    SetNull,
    Cascade,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Relation {
    pub name: String,
    pub model_a_id: String,
    pub model_b_id: String,
    pub model_a_on_delete: OnDelete,
    pub model_b_on_delete: OnDelete,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PrismaEnum {
    name: String,
    values: Vec<String>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTemplate {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<FieldTemplate>,
    pub manifestation: Option<ModelManifestation>,
}

impl ModelTemplate {
    pub fn build(self, schema: Rc<RefCell<Schema>>) -> Rc<RefCell<Model>> {
        let model = Rc::new(RefCell::new(Model {
            name: self.name,
            stable_identifier: self.stable_identifier,
            is_embedded: self.is_embedded,
            fields: Vec::new(),
            manifestation: self.manifestation,
            schema: schema,
        }));

        self.fields
            .into_iter()
            .for_each(|fi| model.borrow_mut().fields.push(fi.build(model.clone())));

        model
    }
}

#[derive(Debug)]
pub struct Model {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<Field>,
    pub manifestation: Option<ModelManifestation>,
    pub schema: Rc<RefCell<Schema>>,
}

impl Model {
    pub fn add_field(&mut self, field: Field) {
        self.fields.push(field);
    }
}

impl Renameable for Model {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

impl Model {
    pub fn find_field(&self, name: &str) -> Option<&ScalarField> {
        self.scalar_fields()
            .iter()
            .find(|field| field.db_name() == name)
            .map(|field| *field)
    }

    pub fn scalar_fields(&self) -> Vec<&ScalarField> {
        self.fields.iter().fold(Vec::new(), |mut acc, field| {
            if let Field::Scalar(scalar_field) = field {
                acc.push(scalar_field);
            }

            acc
        })
    }

    pub fn is_legacy(&self) -> bool {
        self.schema.borrow().is_legacy()
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelManifestation {
    pub db_name: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FieldManifestation {
    pub db_name: String,
}

#[derive(Debug, Deserialize, Clone, Copy)]
#[serde(rename_all = "camelCase")]
pub enum RelationSide {
    A,
    B,
}

impl RelationSide {
    pub fn opposite(&self) -> RelationSide {
        match self {
            RelationSide::A => RelationSide::B,
            RelationSide::B => RelationSide::A,
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy)]
pub enum IdStrategy {
    Auto,
    None,
    Sequence,
}

#[derive(Debug, Serialize, Deserialize, Clone, Copy)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Sequence {
    pub name: String,
    pub initial_value: i32,
    pub allocation_size: i32,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", tag = "type")]
pub enum FieldBehaviour {
    CreatedAt,
    UpdatedAt,
    Id {
        strategy: IdStrategy,
        sequence: Option<Sequence>,
    },
    ScalarList {
        strategy: ScalarListStrategy,
    },
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScalarFieldTemplate {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub behaviour: Option<FieldBehaviour>,
}

#[derive(Debug)]
pub struct ScalarField {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub behaviour: Option<FieldBehaviour>,
    pub model: Rc<RefCell<Model>>,
}

impl Renameable for ScalarField {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

impl ScalarField {
    pub fn is_id(&self) -> bool {
        if self.model.borrow().is_legacy() {
            self.name == ID_FIELD || self.name == EMBEDDED_ID_FIELD
        } else {
            unimplemented!()
        }
    }

    pub fn is_created_at(&self) -> bool {
        if self.model.borrow().is_legacy() {
            self.name == CREATED_AT_FIELD
        } else {
            unimplemented!()
        }
    }

    pub fn is_updated_at(&self) -> bool {
        if self.model.borrow().is_legacy() {
            self.name == UPDATED_AT_FIELD
        } else {
            unimplemented!()
        }
    }

    pub fn is_writable(&self) -> bool {
        !self.is_readonly && !self.is_id() && !self.is_created_at() && !self.is_updated_at()
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationFieldTemplate {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub relation_name: String,
    pub relation_side: RelationSide,
}

#[derive(Debug)]
pub struct RelationField {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub relation_name: String,
    pub relation_side: RelationSide,
    pub model: Rc<RefCell<Model>>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", untagged)]
pub enum FieldTemplate {
    Scalar(ScalarFieldTemplate),
    Relation(RelationFieldTemplate),
}

impl FieldTemplate {
    pub fn build(self, model: Rc<RefCell<Model>>) -> Field {
        match self {
            FieldTemplate::Scalar(st) => {
                let scalar = ScalarField {
                    name: st.name,
                    type_identifier: st.type_identifier,
                    is_required: st.is_required,
                    is_list: st.is_list,
                    is_unique: st.is_unique,
                    is_hidden: st.is_hidden,
                    is_readonly: st.is_readonly,
                    is_auto_generated: st.is_auto_generated,
                    manifestation: st.manifestation,
                    behaviour: st.behaviour,
                    model: model,
                };

                Field::Scalar(scalar)
            }
            FieldTemplate::Relation(rt) => {
                let relation = RelationField {
                    name: rt.name,
                    type_identifier: rt.type_identifier,
                    is_required: rt.is_required,
                    is_list: rt.is_list,
                    is_unique: rt.is_unique,
                    is_hidden: rt.is_hidden,
                    is_readonly: rt.is_readonly,
                    is_auto_generated: rt.is_auto_generated,
                    manifestation: rt.manifestation,
                    relation_name: rt.relation_name,
                    relation_side: rt.relation_side,
                    model: model,
                };

                Field::Relation(relation)
            }
        }
    }
}

#[derive(Debug)]
pub enum Field {
    Scalar(ScalarField),
    Relation(RelationField),
}

#[derive(Clone, Copy, Debug, Serialize, Deserialize, PartialEq)]
pub enum TypeIdentifier {
    String,
    Float,
    Boolean,
    Enum,
    Json,
    DateTime,
    GraphQLID,
    UUID,
    Int,
    Relation,
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json;
    use std::fs::File;

    #[test]
    fn test_schema_json_deserialize() {
        let _: SchemaTemplate =
            serde_json::from_reader(File::open("./example_schema.json").unwrap()).unwrap();
    }
}
