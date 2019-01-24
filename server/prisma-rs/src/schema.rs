mod model;
mod relation;
mod prisma_enum;

pub use prisma_enum::PrismaEnum;

pub use model::{
    Model,
    Field,
    TypeIdentifier,
};

pub use relation::{
    Relation,
    OnDelete,
};

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Schema {
    pub models: Vec<Model>,
    pub relations: Vec<Relation>,
    pub enums: Vec<PrismaEnum>,
}
