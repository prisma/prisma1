use super::*;
use crate::{ast, configuration, dml, errors::ErrorCollection};

/// Wrapper for all lift and validation steps
#[derive(Default)]
pub struct ValidationPipeline {
    lifter: LiftAstToDml,
    validator: Validator,
    standardiser: Standardiser,
}

impl ValidationPipeline {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> Self {
        Self::default()
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &[Box<dyn configuration::Source>]) -> ValidationPipeline {
        ValidationPipeline {
            lifter: LiftAstToDml::with_sources(sources),
            validator: Validator::with_sources(sources),
            standardiser: Standardiser::with_sources(sources),
        }
    }

    /// Validates an AST semantically and promotes it to a datamodel/schema.
    ///
    /// This method will attempt to
    /// * Resolve all directives
    /// * Recursively evaluate all functions
    /// * Perform string interpolation
    /// * Resolve and check default values
    /// * Resolve and check all field types
    pub fn validate(&self, ast_schema: &ast::Datamodel) -> Result<dml::Datamodel, ErrorCollection> {
        let mut all_errors = ErrorCollection::new();

        // Phase 0 is parsing.
        // Phase 1 is source block loading.

        // Phase 2: Prechecks.
        if let Err(mut err) = precheck::Precheck::precheck(&ast_schema) {
            all_errors.append(&mut err);
        }

        // Phase 3: Lift AST to DML.
        let mut schema = match self.lifter.lift(ast_schema) {
            Err(mut err) => {
                // Cannot continue on lifter error.
                all_errors.append(&mut err);
                return Err(all_errors);
            }
            Ok(schema) => schema,
        };

        // Phase 4: Validation
        if let Err(mut err) = self.validator.validate(ast_schema, &mut schema) {
            all_errors.append(&mut err);
        }

        // TODO: Move consistency stuff into different module.
        // Phase 5: Consistency fixes. These don't fail.
        if let Err(mut err) = self.standardiser.standardise(ast_schema, &mut schema) {
            all_errors.append(&mut err);
        }

        if all_errors.has_errors() {
            Err(all_errors)
        } else {
            Ok(schema)
        }
    }
}
