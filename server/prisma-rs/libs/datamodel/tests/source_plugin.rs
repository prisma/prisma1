use datamodel::{
    common::argument::Arguments, dml, dml::FromStrAndSpan, errors::ValidationError, source::*,
    validator::directive::DirectiveValidator,
};
mod common;
use common::*;

//##########################
// Directive implementation
//##########################

struct CustomDirective {
    base_type: dml::ScalarType,
}

impl DirectiveValidator<dml::Field> for CustomDirective {
    fn directive_name(&self) -> &'static str {
        &"mapToBase"
    }
    fn validate_and_apply(&self, _args: &Arguments, obj: &mut dml::Field) -> Result<(), ValidationError> {
        obj.field_type = dml::FieldType::Base(self.base_type);
        return Ok(());
    }
}

//##########################
// Definition Boilerplate
//##########################

struct CustomDbDefinition {}

impl CustomDbDefinition {
    pub fn new() -> CustomDbDefinition {
        CustomDbDefinition {}
    }

    fn get_base_type(&self, arguments: &Arguments) -> Result<dml::ScalarType, ValidationError> {
        if let Ok(arg) = arguments.arg("base_type") {
            dml::ScalarType::from_str_and_span(&arg.as_constant_literal()?, arg.span())
        } else {
            return Ok(dml::ScalarType::String);
        }
    }
}

impl SourceDefinition for CustomDbDefinition {
    fn name(&self) -> &'static str {
        "customDemoSource"
    }

    fn create(&self, name: &str, url: &str, arguments: &Arguments) -> Result<Box<Source>, ValidationError> {
        Ok(Box::new(CustomDb {
            name: String::from(name),
            url: String::from(url),
            base_type: self.get_base_type(arguments)?,
        }))
    }
}

//##########################
// Source Boilerplate
//##########################

struct CustomDb {
    name: String,
    url: String,
    base_type: dml::ScalarType,
}

impl Source for CustomDb {
    fn name(&self) -> &String {
        &self.name
    }
    fn url(&self) -> &String {
        &self.url
    }
    fn get_field_directives(&self) -> Vec<Box<DirectiveValidator<dml::Field>>> {
        vec![Box::new(CustomDirective {
            base_type: self.base_type,
        })]
    }
    fn get_model_directives(&self) -> Vec<Box<DirectiveValidator<dml::Model>>> {
        vec![]
    }
    fn get_enum_directives(&self) -> Vec<Box<DirectiveValidator<dml::Enum>>> {
        vec![]
    }
}

//##########################
// Unit Test Boilerplate
//##########################

#[test]
fn custom_plugin() {
    let dml = r#"

    source custom_1 {
        name = "customDemoSource"
        url = "https://localhost"

        properties {
            base_type = Int
        }
    }

    source custom_2 {
        name = "customDemoSource"
        url = "https://localhost"

        properties {
            base_type = String
        }
    }


    model User {
        firstName: String @custom_1.mapToBase
        lastName: String @custom_1.mapToBase
        email: String
    }

    model Post {
        likes: Int @custom_2.mapToBase
        comments: Int
    }
    "#;

    let schema = parse_with_plugins(dml, vec![Box::new(CustomDbDefinition::new())]);

    let user_model = schema.assert_has_model("User");

    user_model
        .assert_has_field("firstName")
        .assert_base_type(&dml::ScalarType::Int);
    user_model
        .assert_has_field("lastName")
        .assert_base_type(&dml::ScalarType::Int);
    user_model
        .assert_has_field("email")
        .assert_base_type(&dml::ScalarType::String);

    let post_model = schema.assert_has_model("Post");

    post_model
        .assert_has_field("comments")
        .assert_base_type(&dml::ScalarType::Int);
    post_model
        .assert_has_field("likes")
        .assert_base_type(&dml::ScalarType::String);
}
