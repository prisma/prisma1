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

fn whitespace(target: &mut LineWriteable, text: &str) {
    for i in 0..count_lines(text) {
        target.end_line();
    }
}
    
fn comment(target: &mut LineWriteable, comment_text: &str) {
    target.write(&comment_text[0..comment_text.len() - 2]); // slice away line break.
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
                Rule::WHITESPACE => whitespace(target.get_mut(), current.as_str()),
                Rule::COMMENT => comment(target.get_mut(), current.as_str()),
                Rule::model_declaration => unimplemented!(),
                Rule::enum_declaration => unimplemented!(),
                Rule::source_block => unimplemented!(),
                Rule::generator_block => unimplemented!(),
                Rule::type_declaration => unimplemented!(),
                _ => panic!()
            }
        }
    }

    pub fn reformat_model(target: &mut RefCell<Renderer>, token: &Token) {
        let mut table = RefCell::new(TableFormat::new());

        for current in token.clone().into_inner() {
            match current.as_rule() {
                Rule::identifier => { 
                    // Begin.
                    target.get_mut().write(&format!("model {} {{", current.as_str()));
                    target.get_mut().indent_up();
                },
                Rule::directive => unimplemented!(),
                Rule::field_declaration => unimplemented!(),
                Rule::doc_comment => comment(target.get_mut(), current.as_str()),
                Rule::WHITESPACE => {
                    let lines = count_lines(current.as_str());

                    if lines > 1 {
                        // Reset the table layout on more than one newline.
                        table.get_mut().render(target.get_mut());
                        let table = RefCell::new(TableFormat::new());
                    }

                    whitespace(table.get_mut(), current.as_str());
                }
                Rule::COMMENT => comment(table.get_mut(), current.as_str()),
                _ => unreachable!("Encounterd impossible model declaration during parsing: {:?}", current.tokens())
            }
        }

        // End.
        table.get_mut().render(target.get_mut());
        target.get_mut().indent_down();

        target.get_mut().write("}")
    }
    
}

