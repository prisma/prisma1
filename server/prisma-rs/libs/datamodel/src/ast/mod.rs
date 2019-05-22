pub mod parser;

/// AST representation of a prisma datamodel
///
/// This module is used internally to represent an AST. The AST's nodes can be used
/// during validation of a schema, especially when implementing custom directives.
///
/// The AST is not validated, also fields and directives are not resolved. Every node is
/// annotated with it's location in the text representation.
/// Basically, the AST is an object oriented representation of the datamodel's text.

/// Represents a location in a datamodel's text representation.
#[derive(Debug, Clone, Copy)]
pub struct Span {
    pub start: usize,
    pub end: usize,
}

impl Span {
    pub fn new(start: usize, end: usize) -> Span {
        Span { start, end }
    }

    // Creates a new empty span.
    pub fn empty() -> Span {
        Span { start: 0, end: 0 }
    }
    /// Creates a new ast::Span from a pest::Span.
    pub fn from_pest(s: &pest::Span) -> Span {
        Span {
            start: s.start(),
            end: s.end(),
        }
    }
}

impl std::fmt::Display for Span {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "[{} - {}]", self.start, self.end)
    }
}

/// The arity of a field.
#[derive(Debug)]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

/// A comment. Currently unimplemented.
#[derive(Debug)]
pub struct Comment {
    /// The comment text
    pub text: String,
    /// Unused.
    pub is_error: bool,
}

/// An argument, either for directives, or for keys in source blocks.
#[derive(Debug)]
pub struct Argument {
    /// Name of the argument.
    pub name: String,
    /// Argument value.
    pub value: Value,
    /// Location of the argument in the text representation.
    pub span: Span,
}

// TODO: Rename to expression.
/// Represents arbitrary, even nested, expressions.
#[derive(Debug, Clone)]
pub enum Value {
    /// Any numeric value e.g. floats or ints.
    NumericValue(String, Span),
    /// Any boolean value.
    BooleanValue(String, Span),
    /// Any string value.
    StringValue(String, Span),
    /// Any literal constant, basically a string which was not inside "...".
    /// This is used for representing builtin enums.
    ConstantValue(String, Span),
    /// A function with a name and arguments.
    Function(String, Vec<Value>, Span),
}

/// Creates a friendly readable representation for a value's type.
pub fn describe_value_type(val: &Value) -> &'static str {
    match val {
        Value::NumericValue(_, _) => "Numeric",
        Value::BooleanValue(_, _) => "Boolean",
        Value::StringValue(_, _) => "String",
        Value::ConstantValue(_, _) => "Literal",
        Value::Function(_, _, _) => "Functional",
    }
}

/// A directive.
#[derive(Debug)]
pub struct Directive {
    pub name: String,
    pub arguments: Vec<Argument>,
    pub span: Span,
}

/// Trait for an AST node which can have directives.
pub trait WithDirectives {
    fn directives(&self) -> &Vec<Directive>;
}

/// Trait for an AST node which can have comments.
pub trait WithComments {
    fn comments(&self) -> &Vec<Comment>;
}

/// A field declaration.
#[derive(Debug)]
pub struct Field {
    /// The field's type.
    pub field_type: String,
    /// The location of the field's type in the text representation.
    pub field_type_span: Span,
    /// The linked field, in case this is a relation.
    pub field_link: Option<String>,
    /// The name of the field.
    pub name: String,
    /// The aritiy of the field.
    pub arity: FieldArity,
    /// The default value of the field.
    pub default_value: Option<Value>,
    /// The directives of this field.
    pub directives: Vec<Directive>,
    /// The comments for this field.
    pub comments: Vec<Comment>,
    /// The location of this field in the text representation.
    pub span: Span,
}

impl WithDirectives for Field {
    fn directives(&self) -> &Vec<Directive> {
        &self.directives
    }
}

impl WithComments for Field {
    fn comments(&self) -> &Vec<Comment> {
        &self.comments
    }
}

/// An enum declaration.
#[derive(Debug)]
pub struct Enum {
    /// The name of the enum.
    pub name: String,
    /// The values of the enum.
    pub values: Vec<String>,
    /// The directives of this enum.
    pub directives: Vec<Directive>,
    /// The comments for this enum.
    pub comments: Vec<Comment>,
}

impl WithDirectives for Enum {
    fn directives(&self) -> &Vec<Directive> {
        &self.directives
    }
}

impl WithComments for Enum {
    fn comments(&self) -> &Vec<Comment> {
        &self.comments
    }
}

/// A model declaration.
#[derive(Debug)]
pub struct Model {
    /// The name of the model.
    pub name: String,
    /// The fields of the model.
    pub fields: Vec<Field>,
    /// The directives of this model.
    pub directives: Vec<Directive>,
    /// The comments for this model.
    pub comments: Vec<Comment>,
}

impl WithDirectives for Model {
    fn directives(&self) -> &Vec<Directive> {
        &self.directives
    }
}

impl WithComments for Model {
    fn comments(&self) -> &Vec<Comment> {
        &self.comments
    }
}

/// A source block declaration.
#[derive(Debug)]
pub struct SourceConfig {
    /// Name of this source.
    pub name: String,
    /// Top-level configuration properties for this source.
    pub properties: Vec<Argument>,
    /// Detail configuration for this source, found inside the
    /// `properties` block.
    pub detail_configuration: Vec<Argument>,
    /// The comments for this source bloc.
    pub comments: Vec<Comment>,
    /// The location of this source bloc in the text representation.
    pub span: Span,
}

impl WithComments for SourceConfig {
    fn comments(&self) -> &Vec<Comment> {
        &self.comments
    }
}

/// Enum for distinguishing between top-level nodes
/// Enum, Model and SourceConfig.
#[derive(Debug)]
pub enum Top {
    Enum(Enum),
    Model(Model),
    Source(SourceConfig),
}

/// A prisma datamodel.
#[derive(Debug)]
pub struct Schema {
    /// All models, enums, or source config blocks.
    pub models: Vec<Top>,
    /// Top level comments.
    pub comments: Vec<Comment>,
}
