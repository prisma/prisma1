use crate::ast;
use crate::dml::validator::value;

pub struct DirectiveArguments<'a> {
    arguments: &'a Vec<ast::DirectiveArgument>,
}

impl<'a> DirectiveArguments<'a> {
    pub fn new(arguments: &'a Vec<ast::DirectiveArgument>) -> DirectiveArguments {
        DirectiveArguments { arguments: arguments }
    }

    pub fn arg(&self, name: &str) -> Box<value::ValueValidator> {
        for arg in self.arguments {
            if arg.name == name {
                return Box::new(value::WrappedValue {
                    value: arg.value.clone(),
                });
            }
        }
        return Box::new(value::WrappedErrorValue {
            message: format!("Argument '{:?}' not found", name),
        });
    }
}
