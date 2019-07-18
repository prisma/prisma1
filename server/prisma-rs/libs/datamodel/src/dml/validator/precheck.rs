use crate::{
    ast::{self, WithIdentifier},
    common::FromStrAndSpan,
    dml,
    errors::{ErrorCollection, ValidationError},
};

pub struct Precheck {}

impl Precheck {
    pub fn precheck(datamodel: &ast::Datamodel) -> Result<(), ErrorCollection> {
        Self::precheck_datamodel(datamodel)
    }

    fn precheck_datamodel<'a>(datamodel: &'a ast::Datamodel) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();

        let mut top_level_types_checker = DuplicateChecker::new();
        let mut sources_checker = DuplicateChecker::new();
        let mut generators_checker = DuplicateChecker::new();

        for top in &datamodel.models {
            let error = ValidationError::new_duplicate_top_error(
                top.get_type(),
                top.name(),
                top.get_type(),
                &top.identifier().span,
            );
            match top {
                ast::Top::Enum(enum_type) => {
                    Self::assert_is_not_a_reserved_scalar_type(&enum_type.name, &mut errors);
                    top_level_types_checker.check_duplicate(top.name(), error);
                    Self::precheck_enum(&enum_type, &mut errors);
                }
                ast::Top::Model(model) => {
                    Self::assert_is_not_a_reserved_scalar_type(&model.name, &mut errors);
                    top_level_types_checker.check_duplicate(top.name(), error);
                    Self::precheck_model(&model, &mut errors);
                }
                ast::Top::Type(custom_type) => {
                    Self::assert_is_not_a_reserved_scalar_type(&custom_type.name, &mut errors);
                    top_level_types_checker.check_duplicate(top.name(), error);
                }
                ast::Top::Source(source) => {
                    Self::assert_is_not_a_reserved_scalar_type(&source.name, &mut errors);
                    sources_checker.check_duplicate(top.name(), error);
                    Self::precheck_source_config(&source, &mut errors);
                }
                ast::Top::Generator(generator) => {
                    Self::assert_is_not_a_reserved_scalar_type(&generator.name, &mut errors);
                    generators_checker.check_duplicate(top.name(), error);
                    Self::precheck_generator_config(&generator, &mut errors);
                }
            }
        }

        errors.append(&mut top_level_types_checker.errors());
        errors.append(&mut sources_checker.errors());
        errors.append(&mut generators_checker.errors());

        errors.ok()
    }

    fn assert_is_not_a_reserved_scalar_type(identifier: &ast::Identifier, errors: &mut ErrorCollection) {
        if let Ok(_) = dml::ScalarType::from_str_and_span(&identifier.name, &identifier.span) {
            errors.push(ValidationError::new_reserved_scalar_type_error(
                &identifier.name,
                &identifier.span,
            ));
        }
    }

    fn precheck_enum<'a>(enum_type: &'a ast::Enum, errors: &mut ErrorCollection) {
        let mut checker = DuplicateChecker::new();
        for value in &enum_type.values {
            let error = ValidationError::new_duplicate_enum_value_error(&enum_type.name.name, &value.name, &value.span);
            checker.check_duplicate(&value.name, error);
        }
        errors.append(&mut checker.errors());
    }

    fn precheck_model<'a>(model: &'a ast::Model, errors: &mut ErrorCollection) {
        let mut checker = DuplicateChecker::new();
        for field in &model.fields {
            let error = ValidationError::new_duplicate_field_error(
                &model.name.name,
                &field.name.name,
                &field.identifier().span,
            );
            checker.check_duplicate(&field.name.name, error);
        }
        errors.append(&mut checker.errors());
    }

    fn precheck_generator_config<'a>(config: &'a ast::GeneratorConfig, errors: &mut ErrorCollection) {
        let mut checker = DuplicateChecker::new();
        for arg in &config.properties {
            let error = ValidationError::new_duplicate_config_key_error(
                &format!("generator configuration \"{}\"", config.name.name),
                &arg.name.name,
                &arg.identifier().span,
            );
            checker.check_duplicate(&arg.name.name, error);
        }
        errors.append(&mut checker.errors());
    }

    fn precheck_source_config<'a>(config: &'a ast::SourceConfig, errors: &mut ErrorCollection) {
        let mut checker = DuplicateChecker::new();
        for arg in &config.properties {
            let error = ValidationError::new_duplicate_config_key_error(
                &format!("datasource configuration \"{}\"", config.name.name),
                &arg.name.name,
                &arg.identifier().span,
            );
            checker.check_duplicate(&arg.name.name, error);
        }
        errors.append(&mut checker.errors());
    }
}

struct DuplicateChecker<'a> {
    seen: Vec<&'a str>,
    errors: ErrorCollection,
}

impl<'a> DuplicateChecker<'a> {
    fn new() -> DuplicateChecker<'static> {
        DuplicateChecker {
            seen: Vec::new(),
            errors: ErrorCollection::new(),
        }
    }

    fn check_duplicate(&mut self, name: &'a str, error: ValidationError) {
        let already_exists = self.seen.iter().find(|x| *x == &name).is_some();
        if already_exists {
            self.errors.push(error);
        } else {
            self.seen.push(&name);
        }
    }

    fn errors(self) -> ErrorCollection {
        self.errors
    }
}
