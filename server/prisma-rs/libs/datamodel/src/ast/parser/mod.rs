use pest::Parser;

// This is how PEG grammars work:
// https://pest.rs/book/grammars/peg.html

// This is the basic syntax of Pest grammar files:
// https://pest.rs/book/grammars/syntax.html#cheat-sheet

#[derive(Parser)]
#[grammar = "ast/parser/datamodel.pest"]
pub struct PrismaDatamodelParser;

use crate::ast::*;
use crate::errors::ValidationError;

fn parse_string_literal(token: &pest::iterators::Pair<'_, Rule>) -> String {
    return match_first! { token, current,
        Rule::string_content => current.as_str().to_string(),
        _ => unreachable!("Encountered impossible string content during parsing: {:?}", current.tokens())
    };
}

// Expressions

/// Parses an expression, given a Pest parser token.
pub fn parse_expression(token: &pest::iterators::Pair<'_, Rule>) -> Value {
    return match_first! { token, current,
        Rule::numeric_literal => Value::NumericValue(current.as_str().to_string(), Span::from_pest(&current.as_span())),
        Rule::string_literal => Value::StringValue(parse_string_literal(&current), Span::from_pest(&current.as_span())),
        Rule::boolean_literal => Value::BooleanValue(current.as_str().to_string(), Span::from_pest(&current.as_span())),
        Rule::constant_Literal => Value::ConstantValue(current.as_str().to_string(), Span::from_pest(&current.as_span())),
        Rule::function => parse_function(&current),
        _ => unreachable!("Encounterd impossible literal during parsing: {:?}", current.tokens())
    };
}

fn parse_function(token: &pest::iterators::Pair<'_, Rule>) -> Value {
    let mut name: Option<String> = None;
    let mut arguments: Vec<Value> = vec![];

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::argument_value => arguments.push(parse_arg_value(&current)),
        _ => unreachable!("Encounterd impossible function during parsing: {:?}", current.tokens())
    };

    return match name {
        Some(name) => Value::Function(name, arguments, Span::from_pest(&token.as_span())),
        _ => unreachable!("Encounterd impossible function during parsing: {:?}", token.as_str()),
    };
}

fn parse_arg_value(token: &pest::iterators::Pair<'_, Rule>) -> Value {
    return match_first! { token, current,
        Rule::expression => parse_expression(&current),
        _ => unreachable!("Encounterd impossible value during parsing: {:?}", current.tokens())
    };
}

// Directive parsing
fn parse_directive_default_arg(token: &pest::iterators::Pair<'_, Rule>, arguments: &mut Vec<Argument>) {
    match_children! { token, current,
        Rule::argument_value => arguments.push(Argument {
            name: String::from(""),
            value: parse_arg_value(&current),
            span: Span::from_pest(&current.as_span())
        }),
        _ => unreachable!("Encounterd impossible directive default argument during parsing: {:?}", current.tokens())
    };
}

fn parse_directive_arg(token: &pest::iterators::Pair<'_, Rule>) -> Argument {
    let mut name: Option<String> = None;
    let mut argument: Option<Value> = None;

    match_children! { token, current,
        Rule::argument_name => name = Some(current.as_str().to_string()),
        Rule::argument_value => argument = Some(parse_arg_value(&current)),
        _ => unreachable!("Encounterd impossible directive argument during parsing: {:?}", current.tokens())
    };

    return match (name, argument) {
        (Some(name), Some(value)) => Argument {
            name: name,
            value: value,
            span: Span::from_pest(&token.as_span()),
        },
        _ => panic!(
            "Encounterd impossible directive arg during parsing: {:?}",
            token.as_str()
        ),
    };
}

fn parse_directive_args(token: &pest::iterators::Pair<'_, Rule>, arguments: &mut Vec<Argument>) {
    match_children! { token, current,
        Rule::argument => arguments.push(parse_directive_arg(&current)),
        _ => unreachable!("Encounterd impossible directive argument during parsing: {:?}", current.tokens())
    }
}

fn parse_directive(token: &pest::iterators::Pair<'_, Rule>) -> Directive {
    let mut name: Option<String> = None;
    let mut arguments: Vec<Argument> = vec![];

    match_children! { token, current,
        Rule::directive_name => name = Some(current.as_str().to_string()),
        Rule::directive_arguments => parse_directive_args(&current, &mut arguments),
        Rule::directive_single_argument => parse_directive_default_arg(&current, &mut arguments),
        _ => unreachable!("Encounterd impossible directive during parsing: {:?}", current.tokens())
    };

    return match name {
        Some(name) => Directive {
            name,
            arguments,
            span: Span::from_pest(&token.as_span()),
        },
        _ => panic!("Encounterd impossible type during parsing: {:?}", token.as_str()),
    };
}

// Base type parsing
fn parse_base_type(token: &pest::iterators::Pair<'_, Rule>) -> String {
    return match_first! { token, current,
        Rule::identifier => current.as_str().to_string(),
        _ => unreachable!("Encounterd impossible type during parsing: {:?}", current.tokens())
    };
}

fn parse_field_type(token: &pest::iterators::Pair<'_, Rule>) -> (FieldArity, String) {
    return match_first! { token, current,
        Rule::optional_type => (FieldArity::Optional, parse_base_type(&current)),
        Rule::base_type => (FieldArity::Required, parse_base_type(&current)),
        Rule::list_type => (FieldArity::List, parse_base_type(&current)),
        _ => unreachable!("Encounterd impossible field during parsing: {:?}", current.tokens())
    };
}

// Field parsing
fn parse_default_value(token: &pest::iterators::Pair<'_, Rule>) -> Value {
    return match_first! { token, current,
        Rule::expression => parse_expression(&current),
        _ => unreachable!("Encounterd impossible value during parsing: {:?}", current.tokens())
    };
}

fn parse_field(token: &pest::iterators::Pair<'_, Rule>) -> Field {
    let mut name: Option<String> = None;
    let mut directives: Vec<Directive> = vec![];
    let mut default_value: Option<Value> = None;
    let mut field_type: Option<((FieldArity, String), Span)> = None;
    let mut field_link: Option<String> = None;

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::field_type => {
            field_type = Some((parse_field_type(&current), Span::from_pest(&current.as_span())))
        },
        Rule::field_link => field_link = Some(current.as_str().to_string()),
        Rule::default_value => default_value = Some(parse_default_value(&current)),
        Rule::directive => directives.push(parse_directive(&current)),
        _ => unreachable!("Encounterd impossible field declaration during parsing: {:?}", current.tokens())
    }

    return match (name, field_type) {
        (Some(name), Some(((arity, field_type), field_type_span))) => Field {
            field_type: field_type,
            field_type_span: field_type_span,
            field_link: field_link,
            name,
            arity,
            default_value,
            directives,
            comments: vec![],
            span: Span::from_pest(&token.as_span()),
        },
        _ => panic!(
            "Encounterd impossible field declaration during parsing: {:?}",
            token.as_str()
        ),
    };
}
// Model parsing
fn parse_model(token: &pest::iterators::Pair<'_, Rule>) -> Model {
    let mut name: Option<String> = None;
    let mut directives: Vec<Directive> = vec![];
    let mut fields: Vec<Field> = vec![];

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::directive => directives.push(parse_directive(&current)),
        Rule::field_declaration => fields.push(parse_field(&current)),
        _ => unreachable!("Encounterd impossible model declaration during parsing: {:?}", current.tokens())
    }

    return match name {
        Some(name) => Model {
            name,
            fields,
            directives,
            comments: vec![],
            span: Span::from_pest(&token.as_span())
        },
        _ => panic!(
            "Encounterd impossible model declaration during parsing: {:?}",
            token.as_str()
        ),
    };
}

// Enum parsing
fn parse_enum(token: &pest::iterators::Pair<'_, Rule>) -> Enum {
    let mut name: Option<String> = None;
    let mut directives: Vec<Directive> = vec![];
    let mut values: Vec<String> = vec![];

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::directive => directives.push(parse_directive(&current)),
        Rule::enum_field_declaration => values.push(current.as_str().to_string()),
        _ => unreachable!("Encounterd impossible enum declaration during parsing: {:?}", current.tokens())
    }

    return match name {
        Some(name) => Enum {
            name,
            values,
            directives,
            comments: vec![],
            span: Span::from_pest(&token.as_span())
        },
        _ => panic!(
            "Encounterd impossible enum declaration during parsing, name is missing: {:?}",
            token.as_str()
        ),
    };
}

fn parse_source_property(token: &pest::iterators::Pair<'_, Rule>) -> Argument {
    let mut name: Option<String> = None;
    let mut value: Option<Value> = None;

    match_children! { token, current,
        Rule::identifier => name = Some(String::from(current.as_str())),
        Rule::expression => value = Some(parse_expression(&current)),
        _ => unreachable!("Encounterd impossible source property declaration during parsing: {:?}", current.tokens())
    }

    return match (name, value) {
        (Some(name), Some(value)) => Argument {
            name: name,
            value: value,
            span: Span::from_pest(&token.as_span()),
        },
        _ => panic!(
            "Encounterd impossible source property declaration during parsing: {:?}",
            token.as_str()
        ),
    };
}

fn parse_source_property_block(token: &pest::iterators::Pair<'_, Rule>) -> Vec<Argument> {
    let mut properties: Vec<Argument> = vec![];

    match_children! { token, current,
        Rule::source_key_value => properties.push(parse_source_property(&current)),
        _ => unreachable!("Encounterd impossible source property block declaration during parsing: {:?}", current.tokens())
    }

    return properties;
}

// Source parsing
fn parse_source(token: &pest::iterators::Pair<'_, Rule>) -> SourceConfig {
    let mut name: Option<String> = None;
    let mut properties: Vec<Argument> = vec![];
    let mut detail_configuration: Vec<Argument> = vec![];

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::source_key_value => properties.push(parse_source_property(&current)),
        Rule::source_properties => detail_configuration = parse_source_property_block(&current),
        _ => unreachable!("Encounterd impossible source declaration during parsing: {:?}", current.tokens())
    }

    return match name {
        Some(name) => SourceConfig {
            name,
            properties,
            detail_configuration,
            comments: vec![],
            span: Span::from_pest(&token.as_span()),
        },
        _ => panic!(
            "Encounterd impossible source declaration during parsing, name is missing: {:?}",
            token.as_str()
        ),
    };
}

// Whole datamodel parsing

/// Parses a Prisma V2 datamodel document into an internal AST representation.
pub fn parse(datamodel_string: &str) -> Result<Schema, ValidationError> {
    let datamodel_result = PrismaDatamodelParser::parse(Rule::datamodel, datamodel_string);

    match datamodel_result {
        Ok(mut datamodel_wrapped) => {
            let datamodel = datamodel_wrapped.next().unwrap();
            let mut models: Vec<Top> = vec![];

            match_children! { datamodel, current,
                Rule::model_declaration => models.push(Top::Model(parse_model(&current))),
                Rule::enum_declaration => models.push(Top::Enum(parse_enum(&current))),
                Rule::source_block => models.push(Top::Source(parse_source(&current))),
                Rule::EOI => {},
                _ => panic!("Encounterd impossible datamodel declaration during parsing: {:?}", current.tokens())
            }

            Ok(Schema {
                models,
                comments: vec![],
            })
        }
        Err(err) => match err.location {
            pest::error::InputLocation::Pos(pos) => Err(ValidationError::new_parser_error(
                "Unexpected token.",
                &Span::new(pos, pos),
            )),
            pest::error::InputLocation::Span((from, to)) => Err(ValidationError::new_parser_error(
                "Unexpected token.",
                &Span::new(from, to),
            )),
        },
    }
}
