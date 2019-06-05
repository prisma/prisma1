use crate::{ast, dml, errors::ValidationError};

/// State error message. Seeing this error means something went really wrong internally. It's the datamodel equivalent of a bluescreen.
pub (crate) const STATE_ERROR: &str = "Failed lookup of model or field during internal processing. This means that the internal representation was mutated incorrectly.";

pub(crate) trait FindInAstDatamodel {
    fn find_field(&self, model: &str, field: &str) -> Option<&ast::Field>;
    fn find_model(&self, model: &str) -> Option<&ast::Model>;
    fn find_enum(&self, enum_name: &str) -> Option<&ast::Enum>;
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
}

pub fn model_validation_error(message: &str, model: &dml::Model, ast: &ast::Datamodel) -> ValidationError {
    ValidationError::new_model_validation_error(
        message,
        &model.name,
        &ast.find_model(&model.name).expect(STATE_ERROR).span,
    )
}

pub fn field_validation_error(
    message: &str,
    model: &dml::Model,
    field: &dml::Field,
    ast: &ast::Datamodel,
) -> ValidationError {
    ValidationError::new_model_validation_error(
        message,
        &model.name,
        &ast.find_field(&model.name, &field.name).expect(STATE_ERROR).span,
    )
}

pub fn tie(a_model: &dml::Model, a_field: &dml::Field, b_model: &dml::Model, b_field: &dml::Field) -> bool {
    // Model with lower name wins, if name is equal fall back to field.
    a_model.name < b_model.name || (a_model.name == b_model.name && a_field.name < b_field.name)
}
