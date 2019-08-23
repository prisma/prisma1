use crate::{configuration::*, dml, dml::validator::directive::DirectiveValidator};
pub const MYSQL_SOURCE_NAME: &str = "mysql";

pub struct MySqlSource {
    pub(super) name: String,
    pub(super) url: StringFromEnvVar,
    pub(super) documentation: Option<String>,
}

impl Source for MySqlSource {
    fn connector_type(&self) -> &str {
        MYSQL_SOURCE_NAME
    }

    fn name(&self) -> &String {
        &self.name
    }

    fn config(&self) -> std::collections::HashMap<String, String> {
        std::collections::HashMap::new()
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
        vec![]
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
