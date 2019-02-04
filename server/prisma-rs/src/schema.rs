use crate::project::Renameable;

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Schema {
    pub models: Vec<Model>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
}

impl Schema {
    pub fn find_model(&self, name: &str) -> Option<&Model> {
        self.models.iter().find(|model| model.name == name)
    }
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OnDelete {
    SetNull,
    Cascade,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Relation {
    pub name: String,
    pub model_a_id: String,
    pub model_b_id: String,
    pub model_a_on_delete: OnDelete,
    pub model_b_on_delete: OnDelete,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PrismaEnum {
    name: String,
    values: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Model {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<Field>,
    pub manifestation: Option<ModelManifestation>,
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
            .find(|field| {
                field.db_name() == name
            })
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
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelManifestation {
    pub db_name: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FieldManifestation {
    pub db_name: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub enum RelationSide {
    A,
    B,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
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
}

impl Renameable for ScalarField {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationField {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub relation_name: String,
    pub relation_side: RelationSide,
    pub manifestation: Option<FieldManifestation>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", untagged)]
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
        let _: Schema = serde_json::from_reader(
            File::open("./example_schema.json").unwrap()
        ).unwrap();
    }
}
