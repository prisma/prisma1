
// TODO: Naming
pub trait Attachment : std::fmt::Debug + std::clone::Clone + std::cmp::PartialEq {
    fn default() -> Self;
}

#[derive(Debug, PartialEq, Clone)]
pub struct EmptyAttachment {}

impl Attachment for EmptyAttachment {
    fn default() -> Self { EmptyAttachment {} } 
}

// TODO: Better name
// TODO: Decide which attachments we really need.
pub trait TypePack : std::fmt::Debug + std::clone::Clone + std::cmp::PartialEq {
    type FieldAttachment : Attachment;
    type ModelAttachment : Attachment;
    type EnumAttachment : Attachment;
    type SchemaAttachment : Attachment;
    type RelationAttachment : Attachment;
}

#[derive(Debug, PartialEq, Clone)]
pub struct BuiltinTypePack { }

impl TypePack for BuiltinTypePack {
    type EnumAttachment = EmptyAttachment;
    type ModelAttachment = EmptyAttachment;
    type FieldAttachment = EmptyAttachment;
    type SchemaAttachment = EmptyAttachment;
    type RelationAttachment = EmptyAttachment;
}
