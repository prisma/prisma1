use crate::common::*;
use datamodel::{
    common::FromStrAndSpan, common::PrismaType, configuration::*, dml, errors::ValidationError, Arguments,
    DirectiveValidator,
};

//##########################
// Directive implementation
//##########################

struct CustomDirective {
    base_type: PrismaType,
}

impl DirectiveValidator<dml::Field> for CustomDirective {
    fn directive_name(&self) -> &'static str {
        &"mapToBase"
    }
    fn validate_and_apply(&self, _args: &mut Arguments, obj: &mut dml::Field) -> Result<(), ValidationError> {
        obj.field_type = dml::FieldType::Base(self.base_type);
        return Ok(());
    }

    fn serialize(
        &self,
        _obj: &dml::Field,
        _datamodel: &dml::Datamodel,
    ) -> Result<Option<datamodel::ast::Directive>, ValidationError> {
        Ok(None)
    }
}

//##########################
// Definition Boilerplate
//##########################

const CONNECTOR_NAME: &str = "customDemoSource";

struct CustomDbDefinition {}

impl CustomDbDefinition {
    pub fn new() -> CustomDbDefinition {
        CustomDbDefinition {}
    }

    fn get_base_type(&self, arguments: &mut Arguments) -> Result<PrismaType, ValidationError> {
        if let Ok(arg) = arguments.arg("base_type") {
            PrismaType::from_str_and_span(&arg.as_constant_literal()?, arg.span())
        } else {
            return Ok(PrismaType::String);
        }
    }
}

impl SourceDefinition for CustomDbDefinition {
    fn connector_type(&self) -> &'static str {
        CONNECTOR_NAME
    }

    fn create(
        &self,
        name: &str,
        url: StringFromEnvVar,
        arguments: &mut Arguments,
        documentation: &Option<String>,
    ) -> Result<Box<dyn Source>, ValidationError> {
        Ok(Box::new(CustomDb {
            name: String::from(name),
            url: url,
            base_type: self.get_base_type(arguments)?,
            documentation: documentation.clone(),
        }))
    }
}

//##########################
// Source Boilerplate
//##########################

struct CustomDb {
    name: String,
    url: StringFromEnvVar,
    base_type: PrismaType,
    documentation: Option<String>,
}

impl Source for CustomDb {
    fn connector_type(&self) -> &str {
        CONNECTOR_NAME
    }
    fn name(&self) -> &String {
        &self.name
    }
    fn config(&self) -> std::collections::HashMap<String, String> {
        let mut config = std::collections::HashMap::new();

        config.insert(String::from("base_type"), self.base_type.to_string());

        config
    }
    fn url(&self) -> &StringFromEnvVar {
        &self.url
    }
    fn set_url(&mut self, url: &str) {
        self.url = StringFromEnvVar {
            from_env_var: None,
            value: url.to_string(),
        };
    }
    fn get_field_directives(&self) -> Vec<Box<dyn DirectiveValidator<dml::Field>>> {
        vec![Box::new(CustomDirective {
            base_type: self.base_type,
        })]
    }
    fn get_model_directives(&self) -> Vec<Box<dyn DirectiveValidator<dml::Model>>> {
        vec![]
    }
    fn get_enum_directives(&self) -> Vec<Box<dyn DirectiveValidator<dml::Enum>>> {
        vec![]
    }
    fn documentation(&self) -> &Option<String> {
        &self.documentation
    }
}

//##########################
// Unit Test
//##########################

#[test]
fn custom_plugin() {
    std::env::set_var("URL_CUSTOM_1", "https://localhost");
    let schema = parse_with_plugins(DATAMODEL, vec![Box::new(CustomDbDefinition::new())]);

    let user_model = schema.assert_has_model("User");

    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::Int);
    user_model
        .assert_has_field("lastName")
        .assert_base_type(&PrismaType::Int);
    user_model
        .assert_has_field("email")
        .assert_base_type(&PrismaType::String);

    let post_model = schema.assert_has_model("Post");

    post_model
        .assert_has_field("comments")
        .assert_base_type(&PrismaType::Int);
    post_model
        .assert_has_field("likes")
        .assert_base_type(&PrismaType::String);
}

const DATAMODEL: &str = r#"
datasource custom_1 {
    provider = "customDemoSource"
    url = env("URL_CUSTOM_1")
    base_type = Int
}

datasource custom_2 {
    provider = "customDemoSource"
    url = "https://localhost"
    base_type = String
}


model User {
    id Int @id
    firstName String @custom_1.mapToBase
    lastName String @custom_1.mapToBase
    email String
}

model Post {
    id Int @id
    likes Int @custom_2.mapToBase
    comments Int
}
"#;

#[test]
fn serialize_sources_to_dmmf() {
    std::env::set_var("URL_CUSTOM_1", "https://localhost");
    let config =
        datamodel::load_configuration_with_plugins(DATAMODEL, vec![Box::new(CustomDbDefinition::new())]).unwrap();
    let rendered = datamodel::render_sources_to_json(&config.datasources);

    let expected = r#"[
  {
    "name": "custom_1",
    "connectorType": "customDemoSource",
    "url": {
        "fromEnvVar": "URL_CUSTOM_1",
        "value": "https://localhost"       
    },
    "config": {
      "base_type": "Int"
    }
  },
  {
    "name": "custom_2",
    "connectorType": "customDemoSource",
    "url": {
        "fromEnvVar": null,
        "value": "https://localhost"      
    },
    "config": {
      "base_type": "String"
    }
  }
]"#;

    println!("{}", rendered);

    assert_eq_json(&rendered, expected);
}

fn assert_eq_json(a: &str, b: &str) {
    let json_a: serde_json::Value = serde_json::from_str(a).expect("The String a was not valid JSON.");
    let json_b: serde_json::Value = serde_json::from_str(b).expect("The String b was not valid JSON.");

    assert_eq!(json_a, json_b);
}
