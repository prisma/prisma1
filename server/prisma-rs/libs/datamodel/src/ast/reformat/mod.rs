use pest::Parser;
use super::{parser::*, renderer::*, table::*, string_builder::*};

// We have to use RefCell as rust cannot
// do multiple mutable borrows inside a match statement.
use std::cell::RefCell;

type Token<'a> = pest::iterators::Pair<'a, Rule>;
type Tokens<'a> = pest::iterators::Pairs<'a, Rule>;

pub struct Reformatter {
}

fn count_lines(text: &str) -> usize {
    text.as_bytes().iter().filter(|&&c| c == b'\n').count()
}

fn newlines(target: &mut LineWriteable, text: &str, indicator: &str) {
    for i in 0..count_lines(text) {
        target.write(indicator);
        target.end_line();
    }
}
    
fn comment(target: &mut LineWriteable, comment_text: &str) {
    target.write(&comment_text[0..comment_text.len() - 1]); // slice away line break.
    target.end_line();
}

impl Reformatter {

    pub fn reformat_to(input: &str, output: &mut std::io::Write, ident_width: usize) {
        let mut ast = PrismaDatamodelParser::parse(Rule::datamodel, input).unwrap(); // TODO: Handle error.
        let mut top_formatter = RefCell::new(Renderer::new(output, ident_width));
        Self::reformat_top(&mut top_formatter, &ast.next().unwrap());
    }

    pub fn reformat_top(target: &mut RefCell<Renderer>, token: &Token) {
        for current in token.clone().into_inner() {
            match current.as_rule() {
                Rule::WHITESPACE => newlines(target.get_mut(), current.as_str(), "d"),
                Rule::COMMENT => comment(target.get_mut(), current.as_str()),
                Rule::model_declaration => Self::reformat_model(target, &current),
                Rule::enum_declaration => unimplemented!(),
                Rule::source_block => unimplemented!(),
                Rule::generator_block => unimplemented!(),
                Rule::type_declaration => unimplemented!(),
                Rule::EOI => { }
                _ => unreachable!("Encounterd impossible datamodel declaration during parsing: {:?}", current.tokens())
          
            }
        }
    }

    pub fn reformat_model(target: &mut RefCell<Renderer>, token: &Token) {
        let mut table = RefCell::new(TableFormat::new());

        for current in token.clone().into_inner() {
            match current.as_rule() {
                Rule::MODEL_KEYWORD => { },
                Rule::BLOCK_OPEN => { },
                Rule::BLOCK_CLOSE => { },

                Rule::identifier => { 
                    // Begin.
                    target.get_mut().write(&format!("model {} {{", current.as_str()));
                    target.get_mut().indent_up();
                },
                Rule::directive => Self::reformat_directive(
                    target.get_mut(), 
                    &current, "@@"),
                Rule::field_declaration => Self::reformat_field(&mut table, &current),
                // Doc comments are to be placed OUTSIDE of table block.
                Rule::doc_comment => comment(target.get_mut(), current.as_str()),
                Rule::WHITESPACE => {
                    let lines = count_lines(current.as_str());

                    if lines > 1 {
                        // Reset the table layout on more than one newline.
                        table.get_mut().render(target.get_mut());
                        let table = RefCell::new(TableFormat::new());
                    }

                    newlines(table.get_mut(), current.as_str(), "m");
                }
                Rule::COMMENT => comment(&mut table.get_mut().interleave_writer(), current.as_str()),
                _ => unreachable!("Encounterd impossible model declaration during parsing: {:?}", current.tokens())
            }
        }

        // End.
        table.get_mut().render(target.get_mut());
        target.get_mut().indent_down();
        target.get_mut().write("}")
    }
    
    pub fn reformat_field(target: &mut RefCell<TableFormat>, token: &Token) {
        let mut identifier = None;

        for current in token.clone().into_inner() {
            match current.as_rule() {
                Rule::identifier => identifier = Some(String::from(current.as_str())),
                Rule::field_type => {
                    target.get_mut().write(&identifier.clone().expect("Unknown field identifier."));
                    target.get_mut().write(&Self::reformat_field_type(&current));
                },
                Rule::directive => Self::reformat_directive(
                    &mut target.get_mut().column_locked_writer_for(2), 
                    &current, "@"),
                Rule::doc_comment => comment(&mut target.get_mut().interleave_writer(), current.as_str()),
                Rule::COMMENT => comment(&mut target.get_mut().interleave_writer(), current.as_str()),
                Rule::WHITESPACE => newlines(target.get_mut(), current.as_str(), "f"),
                _ => unreachable!("Encounterd impossible field during parsing: {:?}", current.tokens())
            }
        }
    }

    pub fn reformat_field_type(token: &Token) -> String {
        let mut builder = StringBuilder::new();

        for current in token.clone().into_inner() {
            builder.write(&Self::get_identifier(&current));
            match current.as_rule() {
                Rule::optional_type => builder.write("?"),
                Rule::base_type => { },
                Rule::list_type => builder.write("[]"),
                _ => unreachable!("Encounterd impossible field type during parsing: {:?}", current.tokens())
            }
        }

        return builder.to_string();
    }

    pub fn get_identifier(token: &Token) -> String {
        for current in token.clone().into_inner() {
            match current.as_rule() {
                Rule::identifier => return current.as_str().to_string(),
                _ => { }
            };
        }

        panic!("No identifier found.")
    }

    pub fn reformat_directive(target: &mut LineWriteable, token: &Token, owl: &str) {
        for current in token.clone().into_inner() {
            match current.as_rule() {
                Rule::directive_name => {
                    // Begin
                    if !target.line_empty() {
                        target.write(" ");
                    }
                    target.write(owl);
                    target.write(current.as_str());
                },
                Rule::WHITESPACE => { },
                Rule::COMMENT => panic!("Comments inside attributes not supported."),
                Rule::directive_arguments => unimplemented!(),
                _ => unreachable!("Encounterd impossible directive during parsing: {:?}", current.tokens())
            }
        }
    }
}

