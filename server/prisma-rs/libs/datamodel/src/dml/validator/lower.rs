use super::DirectiveBox;
use crate::errors::ErrorCollection;
use crate::source;
use crate::{ast, dml};

pub struct LowerDmlToAst {
    directives: DirectiveBox,
}

impl LowerDmlToAst {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> LowerDmlToAst {
        LowerDmlToAst {
            directives: DirectiveBox::new(),
        }
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &Vec<Box<source::Source>>) -> LowerDmlToAst {
        LowerDmlToAst {
            directives: DirectiveBox::with_sources(sources),
        }
    }

    pub fn lower(&self, schema: &dml::Datamodel) -> Result<ast::Datamodel, ErrorCollection> {
        let mut tops: Vec<ast::Top> = Vec::new();
        let mut errors = ErrorCollection::new();

        for model in schema.models() {
            match self.lower_model(model) {
                Ok(res) => tops.push(ast::Top::Model(res)),
                Err(mut err) => errors.append(&mut err),
            }
        }

        for enm in schema.enums() {
            match self.lower_enum(enm) {
                Ok(res) => tops.push(ast::Top::Enum(res)),
                Err(mut err) => errors.append(&mut err),
            }
        }

        Ok(ast::Datamodel {
            models: tops,
            comments: vec![],
        })
    }

    fn lower_model(&self, model: &dml::Model) -> Result<ast::Model, ErrorCollection> {
        let mut errors = ErrorCollection::new();
        let mut fields: Vec<ast::Field> = Vec::new();

        for field in model.fields() {
            match self.lower_field(field) {
                Ok(ast_field) => fields.push(ast_field),
                Err(mut err) => errors.append(&mut err),
            };
        }

        if errors.has_errors() {
            return Err(errors);
        }

        Ok(ast::Model {
            name: model.name.clone(),
            fields: fields,
            directives: self.directives.model.serialize(model)?,
            comments: vec![],
            span: ast::Span::empty(),
        })
    }

    fn lower_enum(&self, enm: &dml::Enum) -> Result<ast::Enum, ErrorCollection> {
        Ok(ast::Enum {
            name: enm.name.clone(),
            values: enm.values.clone(),
            directives: self.directives.enm.serialize(enm)?,
            comments: vec![],
            span: ast::Span::empty(),
        })
    }

    fn lower_field(&self, field: &dml::Field) -> Result<ast::Field, ErrorCollection> {
        Ok(ast::Field {
            name: field.name.clone(),
            arity: self.lower_field_arity(&field.arity),
            default_value: field.default_value.clone().map(|v| v.into()),
            directives: self.directives.field.serialize(field)?,
            field_type: self.lower_type(&field.field_type),
            comments: vec![],
            field_link: None, // Deprecated
            field_type_span: ast::Span::empty(),
            span: ast::Span::empty(),
        })
    }

    /// Internal: Lowers a field's arity.
    fn lower_field_arity(&self, field_arity: &dml::FieldArity) -> ast::FieldArity {
        match field_arity {
            dml::FieldArity::Required => ast::FieldArity::Required,
            dml::FieldArity::Optional => ast::FieldArity::Optional,
            dml::FieldArity::List => ast::FieldArity::List,
        }
    }

    /// Internal: Lowers a field's arity.
    fn lower_type(&self, field_type: &dml::FieldType) -> String {
        match field_type {
            dml::FieldType::Base(tpe) => tpe.to_string(),
            dml::FieldType::Enum(tpe) => tpe.clone(),
            dml::FieldType::Relation(rel) => rel.to.clone(),
            _ => unimplemented!("Connector specific types are not supported atm."),
        }
    }
}
