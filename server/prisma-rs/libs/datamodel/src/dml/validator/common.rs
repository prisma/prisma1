use crate::ast;

/// State error message. Seeing this error means something went really wrong internally. It's the datamodel equivalent of a bluescreen.
pub (crate) const STATE_ERROR: &str = "Failed lookup of model or field during internal processing. This means that the internal representation was mutated incorrectly.";

pub(crate) trait FindInAstDatamodel {
    fn find_field(&self, field: &str, field: &str) -> Option<&ast::Field>;
    fn find_model(&self, model: &str) -> Option<&ast::Model>;
    fn find_enum(&self, enum_name: &str) -> Option<&ast::Enum>;
    fn find_custom_type(&self, type_name: &str) -> Option<&ast::Field>;
}

impl FindInAstDatamodel for ast::Datamodel {
    fn find_field(&self, model: &str, field: &str) -> Option<&ast::Field> {
        for ast_field in &self.find_model(model)?.fields {
            if ast_field.name == field {
                return Some(&ast_field);
            }
        }

        None
    }

    fn find_model(&self, model: &str) -> Option<&ast::Model> {
        for ast_top in &self.models {
            if let ast::Top::Model(ast_model) = ast_top {
                if ast_model.name == model {
                    return Some(&ast_model);
                }
            }
        }

        None
    }

    fn find_enum(&self, enum_name: &str) -> Option<&ast::Enum> {
        for ast_top in &self.models {
            if let ast::Top::Enum(ast_enum) = ast_top {
                if ast_enum.name == enum_name {
                    return Some(&ast_enum);
                }
            }
        }

        None
    }

    fn find_custom_type(&self, type_name: &str) -> Option<&ast::Field> {
        for ast_top in &self.models {
            if let ast::Top::Type(ast_type) = ast_top {
                if ast_type.name == type_name {
                    return Some(&ast_type);
                }
            }
        }

        None
    }
}

impl ast::WithDirectives for Vec<ast::Directive> {
    fn directives(&self) -> &Vec<ast::Directive> {
        self
    }
}