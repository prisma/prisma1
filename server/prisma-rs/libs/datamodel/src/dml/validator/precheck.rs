use crate::{
    ast::{self, WithSpan},
    errors::ErrorCollection,
    errors::ValidationError,
};
use std::collections::HashMap;

pub struct Precheck {}

impl Precheck {
    pub fn precheck(datamodel: &ast::Datamodel) -> Result<(), ErrorCollection> {
        Self::precheck_datamodel(datamodel)
    }

    pub fn precheck_datamodel<'a>(datamodel: &'a ast::Datamodel) -> Result<(), ErrorCollection> {
        let mut models = HashMap::<&'a str, &'a ast::Top>::new();
        let mut sources = HashMap::<&'a str, &'a ast::Top>::new();
        let mut generators = HashMap::<&'a str, &'a ast::Top>::new();
        let mut errors = ErrorCollection::new();

        for top in &datamodel.models {
            match top {
                ast::Top::Enum(enum_type) => {
                    models.check_duplicate("", &enum_type.name, &top, &mut errors);
                    Self::precheck_enum(&enum_type, &mut errors);
                }
                ast::Top::Model(model) => {
                    models.check_duplicate("", &model.name, &top, &mut errors);
                    Self::precheck_model(&model, &mut errors);
                }
                ast::Top::Type(custom_type) => models.check_duplicate("", &custom_type.name, &top, &mut errors),
                ast::Top::Source(source) => {
                    sources.check_duplicate("", &source.name, &top, &mut errors);
                    Self::precheck_source_config(&source, &mut errors);
                }
                ast::Top::Generator(generator) => {
                    generators.check_duplicate("", &generator.name, &top, &mut errors);
                    Self::precheck_generator_config(&generator, &mut errors);
                }
            }
        }

        errors.ok()
    }

    pub fn precheck_enum<'a>(enum_type: &'a ast::Enum, errors: &mut ErrorCollection) {
        let mut values = HashMap::<&'a str, &'a ast::EnumValue>::new();
        for value in &enum_type.values {
            values.check_duplicate(&enum_type.name, &value.name, &value, errors);
        }
    }

    pub fn precheck_model<'a>(model: &'a ast::Model, errors: &mut ErrorCollection) {
        let mut fields = HashMap::<&'a str, &'a ast::Field>::new();
        for field in &model.fields {
            fields.check_duplicate(&model.name, &field.name, &field, errors);
        }
    }

    pub fn precheck_generator_config<'a>(config: &'a ast::GeneratorConfig, errors: &mut ErrorCollection) {
        let mut args = HashMap::<&'a str, &'a ast::Argument>::new();
        for arg in &config.properties {
            args.check_duplicate(
                &format!("generator configuration {}", config.name),
                &arg.name,
                &arg,
                errors,
            );
        }
    }

    pub fn precheck_source_config<'a>(config: &'a ast::SourceConfig, errors: &mut ErrorCollection) {
        let mut args = HashMap::<&'a str, &'a ast::Argument>::new();
        for arg in &config.properties {
            args.check_duplicate(
                &format!("datasource configuration {}", config.name),
                &arg.name,
                &arg,
                errors,
            );
        }
    }
}

trait CheckDuplicate<K, T> {
    fn check_duplicate(&mut self, parent_name: &str, key: K, obj: T, error: &mut ErrorCollection);
}

// Top
impl<'a> CheckDuplicate<&'a str, &'a ast::Top> for HashMap<&'a str, &'a ast::Top> {
    fn check_duplicate(&mut self, _parent_name: &str, key: &'a str, obj: &'a ast::Top, errors: &mut ErrorCollection) {
        if self.contains_key(key) {
            errors.push(ValidationError::new_duplicate_top_error(
                obj.get_type(),
                self[key].get_type(),
                key,
                obj.span(),
            ));
        } else {
            self.insert(key, obj);
        }
    }
}

// Enum
impl<'a> CheckDuplicate<&'a str, &'a ast::EnumValue> for HashMap<&'a str, &'a ast::EnumValue> {
    fn check_duplicate(
        &mut self,
        parent_name: &str,
        key: &'a str,
        value: &'a ast::EnumValue,
        errors: &mut ErrorCollection,
    ) {
        if self.contains_key(key) {
            errors.push(ValidationError::new_duplicate_enum_value_error(
                parent_name,
                key,
                &value.span,
            ));
        } else {
            self.insert(key, value);
        }
    }
}

// Field
impl<'a> CheckDuplicate<&'a str, &'a ast::Field> for HashMap<&'a str, &'a ast::Field> {
    fn check_duplicate(
        &mut self,
        parent_name: &str,
        key: &'a str,
        field: &'a ast::Field,
        errors: &mut ErrorCollection,
    ) {
        if self.contains_key(key) {
            errors.push(ValidationError::new_duplicate_field_error(
                parent_name,
                key,
                &field.span,
            ));
        } else {
            self.insert(key, field);
        }
    }
}

// Config
impl<'a> CheckDuplicate<&'a str, &'a ast::Argument> for HashMap<&'a str, &'a ast::Argument> {
    fn check_duplicate(
        &mut self,
        parent_name: &str,
        key: &'a str,
        arg: &'a ast::Argument,
        errors: &mut ErrorCollection,
    ) {
        if self.contains_key(key) {
            errors.push(ValidationError::new_duplicate_config_key_error(
                parent_name,
                key,
                &arg.span,
            ));
        } else {
            self.insert(key, arg);
        }
    }
}
