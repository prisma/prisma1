pub mod parser;

#[derive(Debug)]
pub enum FieldArity {
    Required,
    Optional,
    List,
}

#[derive(Debug)]
pub struct Comment {
    pub text: String,
    pub is_error: bool
}

#[derive(Debug)]
pub struct DirectiveArgument {
    pub name: String,
    pub value: Value
}

#[derive(Debug, Clone)]
pub enum Value {
    NumericValue(String),
    BooleanValue(String),
    StringValue(String),
    ConstantValue(String)
}

#[derive(Debug)]
pub struct Directive {
    pub name: String,
    pub arguments: Vec<DirectiveArgument>
}

pub trait WithDirectives {
    fn directives(&self) -> &Vec<Directive>;
}

pub trait WithComments {
    fn comments(&self) -> &Vec<Comment>;
}

#[derive(Debug)]
pub struct Field {
    pub field_type: String,
    pub field_link: Option<String>,
    pub name: String,
    pub arity: FieldArity,
    pub default_value: Option<Value>,
    pub directives: Vec<Directive>,
    pub comments: Vec<Comment>
}

impl WithDirectives for Field {
    fn directives(&self) -> &Vec<Directive> { &self.directives }
}

impl WithComments for Field {
    fn comments(&self) -> &Vec<Comment> { &self.comments }
}

#[derive(Debug)]
pub struct Enum { 
    pub name: String,
    pub values: Vec<String>,
    pub directives: Vec<Directive>,
    pub comments: Vec<Comment>
}

impl WithDirectives for Enum  {
    fn directives(&self) -> &Vec<Directive> { &self.directives }
}

impl WithComments for Enum {
    fn comments(&self) -> &Vec<Comment> { &self.comments }
}

#[derive(Debug)]
pub struct Model {
    pub name: String,
    pub fields: Vec<Field>,
    pub directives: Vec<Directive>,
    pub comments: Vec<Comment>,
}

impl WithDirectives for Model {
    fn directives(&self) -> &Vec<Directive> { &self.directives }
}

impl WithComments for Model  {
    fn comments(&self) -> &Vec<Comment> { &self.comments }
}

#[derive(Debug)]
pub enum ModelOrEnum {
    Enum(Enum),
    Model(Model)
}

#[derive(Debug)]
pub struct Schema {
    pub models: Vec<ModelOrEnum>,
    pub comments: Vec<Comment>
}