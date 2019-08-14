use super::*;
use once_cell::sync::OnceCell;
use prisma_models::{EnumType, InternalDataModelRef, ModelRef, PrismaValue};
use std::{
    borrow::Borrow,
    boxed::Box,
    fmt,
    sync::{Arc, Weak},
};

pub type OutputTypeRef = Arc<OutputType>;

pub type ObjectTypeStrongRef = Arc<ObjectType>;
pub type ObjectTypeRef = Weak<ObjectType>;

pub type InputObjectTypeStrongRef = Arc<InputObjectType>;
pub type InputObjectTypeRef = Weak<InputObjectType>;

pub type QuerySchemaRef = Arc<QuerySchema>;
pub type FieldRef = Arc<Field>;
pub type InputFieldRef = Arc<InputField>;
pub type EnumTypeRef = Arc<EnumType>;

/// The query schema.
/// Defines which operations (query/mutations) are possible on a database, based on the (internal) data model.
///
/// Conceptually, a query schema stores two trees (query / mutation) that consist of
/// input and output types. Special consideration is required when dealing with object types.
///
/// Object types can be referenced multiple times throughout the schema, also recursively, which requires the use
/// of weak references to prevent memory leaks. To simplify the overall management of Arcs and weaks, the
/// query schema is subject to a number of invariants.
/// The most important one is that the only strong references (Arc) to a single object types
/// is only ever held by the top-level QuerySchema struct, never by the trees, which only ever hold weak refs.
///
/// Using a QuerySchema should never involve dealing with the strong references.
#[derive(Debug)]
pub struct QuerySchema {
    pub query: OutputTypeRef,
    pub mutation: OutputTypeRef,

    /// Stores all strong refs to the input object types.
    input_object_types: Vec<InputObjectTypeStrongRef>,

    /// Stores all strong refs to the output object types.
    output_object_types: Vec<ObjectTypeStrongRef>,

    pub internal_data_model: InternalDataModelRef,
}

impl QuerySchema {
    pub fn new(
        query: OutputTypeRef,
        mutation: OutputTypeRef,
        input_object_types: Vec<InputObjectTypeStrongRef>,
        output_object_types: Vec<ObjectTypeStrongRef>,
        internal_data_model: InternalDataModelRef,
    ) -> Self {
        QuerySchema {
            query,
            mutation,
            input_object_types,
            output_object_types,
            internal_data_model,
        }
    }

    pub fn find_mutation_field<T>(&self, name: T) -> Option<FieldRef>
    where
        T: Into<String>,
    {
        let name = name.into();
        self.mutation()
            .get_fields()
            .into_iter()
            .find(|f| f.name == name)
            .cloned()
    }

    pub fn find_query_field<T>(&self, name: T) -> Option<FieldRef>
    where
        T: Into<String>,
    {
        let name = name.into();
        self.query().get_fields().into_iter().find(|f| f.name == name).cloned()
    }

    pub fn mutation(&self) -> ObjectTypeStrongRef {
        match self.mutation.borrow() {
            OutputType::Object(ref o) => o.into_arc(),
            _ => unreachable!(),
        }
    }

    pub fn query(&self) -> ObjectTypeStrongRef {
        match self.query.borrow() {
            OutputType::Object(ref o) => o.into_arc(),
            _ => unreachable!(),
        }
    }

    // WIP
    // pub fn compact(mut self) -> Self {
    //   // Check if there are empty input objects and clean up the AST is there are any.
    //   let (valid_objects, empty_input_objects) = self.input_object_types.into_iter().partition(|i| i.is_empty());
    //   self.input_object_types = valid_objects;

    //   if empty_input_objects.len() > 0 {
    //     // Walk the AST and discard any element where the weak ref upgrade fails.
    //     self.visit_output_type(&self.query);
    //     self.visit_output_type(&self.mutation);
    //   }

    //   self
    // }

    // fn visit_output_type(&self, out: &OutputType) -> VisitorOperation<OutputType> {
    //   match out {
    //     OutputType::Object(obj) => unimplemented!(),
    //     OutputType::Enum(enum_type) => unimplemented!(),
    //     OutputType::List(out) => unimplemented!(),
    //     OutputType::Opt(out) => unimplemented!(),
    //     OutputType::Scalar(s) => unimplemented!(),
    //   }
    // }

    // fn visit(&mut self, visitor: impl SchemaAstVisitor) {
    //   match visitor.visit_output_type(&self.query) {
    //     VisitorOperation::Remove => unimplemented!(),
    //     VisitorOperation::Replace(t) => unimplemented!(),
    //     VisitorOperation::None => unimplemented!(),
    //   };

    //   visitor.visit_output_type(&self.mutation);
    // }
}

#[derive(DebugStub)]
pub struct ObjectType {
    pub name: String,

    #[debug_stub = "#Fields Cell#"]
    pub fields: OnceCell<Vec<FieldRef>>,

    // Object types can directly map to models.
    pub model: Option<ModelRef>,
}

impl ObjectType {
    pub fn get_fields(&self) -> &Vec<FieldRef> {
        self.fields.get().unwrap()
    }

    pub fn set_fields(&self, fields: Vec<Field>) {
        self.fields
            .set(fields.into_iter().map(|f| Arc::new(f)).collect())
            .unwrap();
    }

    pub fn find_field(&self, name: &str) -> Option<FieldRef> {
        self.get_fields().into_iter().find(|f| &f.name == name).cloned()
    }

    /// True if fields are empty, false otherwise.
    pub fn is_empty(&self) -> bool {
        self.get_fields().is_empty()
    }
}

#[derive(Debug)]
pub struct Field {
    pub name: String,
    pub arguments: Vec<Argument>,
    pub field_type: OutputTypeRef,
    pub operation: Option<ModelOperation>,
}

/// Designates a specific top-level operation on a corresponding model.
#[derive(Debug, Clone)]
pub struct ModelOperation {
    pub model: ModelRef,
    pub operation: OperationTag,
}

impl ModelOperation {
    pub fn new(model: ModelRef, operation: OperationTag) -> Self {
        Self { model, operation }
    }
}

/// Designates which operation is intended for a query and in case of non-read
/// operations, which appropriate operation should be used to query the output.
#[derive(Debug, Clone, PartialEq)]
pub enum OperationTag {
    /// Read operations.
    FindOne,
    FindMany,

    /// Write operations with associated result operations.
    CreateOne(Box<OperationTag>),
    UpdateOne(Box<OperationTag>),
    UpdateMany(Box<OperationTag>),
    DeleteOne(Box<OperationTag>),
    DeleteMany(Box<OperationTag>),
    UpsertOne(Box<OperationTag>),

    /// Marks an operation to write the result of the previous query directly
    /// as map shaped as the defined output type of a query.
    /// This is a temporary workaround until the serialization has been reworked.
    CoerceResultToOutputType,
}

impl fmt::Display for OperationTag {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        let s = match self {
            OperationTag::FindOne => "findOne",
            OperationTag::FindMany => "findMany",
            OperationTag::CreateOne(_) => "createOne",
            OperationTag::UpdateOne(_) => "updateOne",
            OperationTag::UpdateMany(_) => "updateMany",
            OperationTag::DeleteOne(_) => "deleteOne",
            OperationTag::DeleteMany(_) => "deleteMany",
            OperationTag::UpsertOne(_) => "upsertOne",
            OperationTag::CoerceResultToOutputType => unreachable!(), // Only top-level ops are reached.
        };

        s.fmt(f)
    }
}

#[derive(Debug)]
pub struct Argument {
    pub name: String,
    pub argument_type: InputType,
    pub default_value: Option<PrismaValue>,
}

#[derive(DebugStub)]
pub struct InputObjectType {
    pub name: String,

    #[debug_stub = "#Input Fields Cell#"]
    pub fields: OnceCell<Vec<InputFieldRef>>,
}

impl InputObjectType {
    pub fn get_fields(&self) -> &Vec<InputFieldRef> {
        self.fields.get().unwrap()
    }

    pub fn set_fields(&self, fields: Vec<InputField>) {
        self.fields
            .set(fields.into_iter().map(|f| Arc::new(f)).collect())
            .unwrap();
    }

    /// True if fields are empty, false otherwise.
    pub fn is_empty(&self) -> bool {
        self.get_fields().is_empty()
    }

    pub fn find_field<T>(&self, name: T) -> Option<InputFieldRef>
    where
        T: Into<String>,
    {
        let name = name.into();
        self.get_fields().into_iter().find(|f| f.name == name).cloned()
    }
}

#[derive(Debug)]
pub struct InputField {
    pub name: String,
    pub field_type: InputType,
    pub default_value: Option<PrismaValue>,
}

#[derive(Debug, Clone)]
pub enum InputType {
    Enum(EnumTypeRef),
    List(Box<InputType>),
    Object(InputObjectTypeRef),
    Opt(Box<InputType>),
    Scalar(ScalarType),
}

impl InputType {
    pub fn list(containing: InputType) -> InputType {
        InputType::List(Box::new(containing))
    }

    pub fn opt(containing: InputType) -> InputType {
        InputType::Opt(Box::new(containing))
    }

    pub fn object(containing: InputObjectTypeRef) -> InputType {
        InputType::Object(containing)
    }

    pub fn string() -> InputType {
        InputType::Scalar(ScalarType::String)
    }

    pub fn int() -> InputType {
        InputType::Scalar(ScalarType::Int)
    }

    pub fn float() -> InputType {
        InputType::Scalar(ScalarType::Float)
    }

    pub fn boolean() -> InputType {
        InputType::Scalar(ScalarType::Boolean)
    }

    pub fn date_time() -> InputType {
        InputType::Scalar(ScalarType::DateTime)
    }

    pub fn json() -> InputType {
        InputType::Scalar(ScalarType::Json)
    }

    pub fn uuid() -> InputType {
        InputType::Scalar(ScalarType::UUID)
    }

    pub fn id() -> InputType {
        InputType::Scalar(ScalarType::ID)
    }
}

#[derive(Debug)]
pub enum OutputType {
    Enum(EnumTypeRef),
    List(OutputTypeRef),
    Object(ObjectTypeRef),
    Opt(OutputTypeRef),
    Scalar(ScalarType),
}

impl OutputType {
    pub fn list(containing: OutputType) -> OutputType {
        OutputType::List(Arc::new(containing))
    }

    pub fn opt(containing: OutputType) -> OutputType {
        OutputType::Opt(Arc::new(containing))
    }

    pub fn object(containing: ObjectTypeRef) -> OutputType {
        OutputType::Object(containing)
    }

    pub fn string() -> OutputType {
        OutputType::Scalar(ScalarType::String)
    }

    pub fn int() -> OutputType {
        OutputType::Scalar(ScalarType::Int)
    }

    pub fn float() -> OutputType {
        OutputType::Scalar(ScalarType::Float)
    }

    pub fn boolean() -> OutputType {
        OutputType::Scalar(ScalarType::Boolean)
    }

    pub fn date_time() -> OutputType {
        OutputType::Scalar(ScalarType::DateTime)
    }

    pub fn json() -> OutputType {
        OutputType::Scalar(ScalarType::Json)
    }

    pub fn uuid() -> OutputType {
        OutputType::Scalar(ScalarType::UUID)
    }

    pub fn id() -> OutputType {
        OutputType::Scalar(ScalarType::ID)
    }

    /// Attempts to recurse through the type until an object type is found.
    /// Returns Some(ObjectTypeStrongRef) if ab object type is found, None otherwise.
    pub fn as_object_type(&self) -> Option<ObjectTypeStrongRef> {
        match self {
            OutputType::Enum(_) => None,
            OutputType::List(inner) => inner.as_object_type(),
            OutputType::Object(obj) => Some(obj.into_arc()),
            OutputType::Opt(inner) => inner.as_object_type(),
            OutputType::Scalar(_) => None,
        }
    }

    pub fn is_list(&self) -> bool {
        match self {
            OutputType::Opt(inner) => inner.is_list(),
            OutputType::List(_) => true,
            _ => false,
        }
    }

    pub fn is_object(&self) -> bool {
        match self {
            OutputType::Opt(inner) => inner.is_object(),
            OutputType::Object(_) => true,
            _ => false,
        }
    }
}

#[derive(Debug, Clone)]
pub enum ScalarType {
    String,
    Int,
    Float,
    Boolean,
    Enum(EnumTypeRef),
    DateTime,
    Json,
    UUID,
    ID,
}

impl From<EnumType> for OutputType {
    fn from(e: EnumType) -> Self {
        OutputType::Enum(Arc::new(e))
    }
}

impl From<EnumType> for InputType {
    fn from(e: EnumType) -> Self {
        InputType::Enum(Arc::new(e))
    }
}
