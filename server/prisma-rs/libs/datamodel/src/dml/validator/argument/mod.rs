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
            println!("{} <> {}", arg.name, name);
            if arg.name == name {
                println!("Returning result");
                return Box::new(value::WrappedValue { value: arg.value.clone() })
            }
        }
        return Box::new(value::WrappedErrorValue {
            message: format!("Argument '{:?}' not found", name),
        });
    }


    pub fn default_arg(&self, name: &str) -> Box<value::ValueValidator> {
        let arg = self.arg(name);

        if(arg.is_valid()) {
            return arg;
        } else {
            // Fallback to default arg without name.
            return self.arg("");
        }
    }
}
