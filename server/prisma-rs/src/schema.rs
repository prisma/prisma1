#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Schema {
    pub models: Vec<Model>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
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
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Field {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
}

#[derive(Clone, Copy, Debug, Serialize, Deserialize)]
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
