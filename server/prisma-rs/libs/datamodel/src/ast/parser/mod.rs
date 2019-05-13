use pest::Parser;

// This is how PEG grammars work:
// https://pest.rs/book/grammars/peg.html

// This is the basic syntax of Pest grammar files:
// https://pest.rs/book/grammars/syntax.html#cheat-sheet

#[derive(Parser)]
#[grammar = "ast/parser/datamodel.pest"]
pub struct PrismaDatamodelParser;

use crate::ast::*;

// Macro to match all children in a parse tree
macro_rules! match_children (
    ($token:ident, $current:ident, $($pattern:pat => $result:expr),*) => (
        // Explicit clone, as into_inner consumes the pair.
        // We only need a reference to the pair later for logging.
        for $current in $token.clone().into_inner() {
            match $current.as_rule() {
                $(
                    $pattern => $result
                ),*
            }
        }
    );
);

// Macro to match the first child in a parse tree
macro_rules! match_first (
    ($token:ident, $current:ident, $($pattern:pat => $result:expr),*) => ( {
            // Explicit clone, as into_inner consumes the pair.
        // We only need a reference to the pair later for logging.
            let $current = $token.clone().into_inner().next().unwrap();
            match $current.as_rule() {
                $(
                    $pattern => $result
                ),*
            }
        }
    );
);

fn parse_string_literal(token: &pest::iterators::Pair<'_, Rule>) -> String {
    return match_first! { token, current,
        Rule::string_content => current.as_str().to_string(),
        _ => unreachable!("Encountered impossible string content during parsing: {:?}", current.tokens())
    };
}

// Literals
fn parse_literal(token: &pest::iterators::Pair<'_, Rule>) -> Value {
    return match_first! { token, current,
        Rule::numeric_literal => Value::NumericValue(current.as_str().to_string()),
        Rule::string_literal => Value::StringValue(parse_string_literal(&current)),
        Rule::boolean_literal => Value::BooleanValue(current.as_str().to_string()),
        Rule::constant_Literal => Value::ConstantValue(current.as_str().to_string()),
        _ => unreachable!("Encounterd impossible literal during parsing: {:?}", current.tokens())
    };
}

// Directive parsing
fn parse_directive_arg_value(token: &pest::iterators::Pair<'_, Rule>) -> Value {
    return match_first! { token, current,
        Rule::any_literal => parse_literal(&current),
        _ => unreachable!("Encounterd impossible value during parsing: {:?}", current.tokens())
    };
}

fn parse_directive_default_arg(token: &pest::iterators::Pair<'_, Rule>, arguments: &mut Vec<DirectiveArgument>) {
    match_children! { token, current,
        Rule::directive_argument_value => arguments.push(DirectiveArgument { name: String::from(""), value: parse_directive_arg_value(&current) }),
        _ => unreachable!("Encounterd impossible directive default argument during parsing: {:?}", current.tokens())
    };
}

fn parse_directive_arg(token: &pest::iterators::Pair<'_, Rule>) -> DirectiveArgument {
    let mut name: Option<String> = None;
    let mut argument: Option<Value> = None;

    match_children! { token, current,
        Rule::directive_argument_name => name = Some(current.as_str().to_string()),
        Rule::directive_argument_value => argument = Some(parse_directive_arg_value(&current)),
        _ => unreachable!("Encounterd impossible directive argument during parsing: {:?}", current.tokens())
    };

    return match (name, argument) {
        (Some(name), Some(value)) => DirectiveArgument {
            name: name,
            value: value,
        },
        _ => panic!(
            "Encounterd impossible directive arg during parsing: {:?}",
            token.as_str()
        ),
    };
}

fn parse_directive_args(token: &pest::iterators::Pair<'_, Rule>, arguments: &mut Vec<DirectiveArgument>) {
    match_children! { token, current,
        Rule::directive_argument => arguments.push(parse_directive_arg(&current)),
        _ => unreachable!("Encounterd impossible directive argument during parsing: {:?}", current.tokens())
    }
}

fn parse_directive(token: &pest::iterators::Pair<'_, Rule>) -> Directive {
    let mut name: Option<String> = None;
    let mut arguments: Vec<DirectiveArgument> = vec![];

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::directive_arguments => parse_directive_args(&current, &mut arguments),
        Rule::directive_single_argument => parse_directive_default_arg(&current, &mut arguments),
        _ => unreachable!("Encounterd impossible directive during parsing: {:?}", current.tokens())
    };

    return match name {
        Some(name) => Directive { name, arguments },
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
        Rule::any_literal => parse_literal(&current),
        _ => unreachable!("Encounterd impossible value during parsing: {:?}", current.tokens())
    };
}

fn parse_field(token: &pest::iterators::Pair<'_, Rule>) -> Field {
    let mut name: Option<String> = None;
    let mut directives: Vec<Directive> = vec![];
    let mut default_value: Option<Value> = None;
    let mut field_type: Option<(FieldArity, String)> = None;
    let mut field_link: Option<String> = None;

    match_children! { token, current,
        Rule::identifier => name = Some(current.as_str().to_string()),
        Rule::field_type => field_type = Some(parse_field_type(&current)),
        Rule::field_link => field_link = Some(current.as_str().to_string()),
        Rule::default_value => default_value = Some(parse_default_value(&current)),
        Rule::directive => directives.push(parse_directive(&current)),
        _ => unreachable!("Encounterd impossible field declaration during parsing: {:?}", current.tokens())
    }

    return match (name, field_type) {
        (Some(name), Some((arity, field_type))) => Field {
            field_type: field_type,
            field_link: field_link,
            name,
            arity,
            default_value,
            directives,
            comments: vec![],
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
        },
        _ => panic!(
            "Encounterd impossible enum declaration during parsing: {:?}",
            token.as_str()
        ),
    };
}

// Whole datamodel parsing
pub fn parse(datamodel_string: &String) -> Schema {
    let datamodel = PrismaDatamodelParser::parse(Rule::datamodel, datamodel_string)
        .expect("Could not parse datamodel file.")
        .next()
        .unwrap();

    let mut models: Vec<ModelOrEnum> = vec![];

    match_children! { datamodel, current,
        Rule::model_declaration => models.push(ModelOrEnum::Model(parse_model(&current))),
        Rule::enum_declaration => models.push(ModelOrEnum::Enum(parse_enum(&current))),
        Rule::EOI => {},
        _ => panic!("Encounterd impossible datamodel declaration during parsing: {:?}", current.tokens())
    }

    return Schema {
        models,
        comments: vec![],
    };
}
