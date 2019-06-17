use super::*;
use crate::{ast, configuration, dml, errors::ErrorCollection};

/// Wrapper for all lift and validation steps
pub struct ValidationPipeline {
    lifter: LiftAstToDml,
    validator: Validator,
    standardiser: Standardiser,
}

impl ValidationPipeline {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> ValidationPipeline {
        ValidationPipeline {
            lifter: LiftAstToDml::new(),
            validator: Validator::new(),
            standardiser: Standardiser::new(),
        }
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &Vec<Box<configuration::Source>>) -> ValidationPipeline {
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
        // Phase 0 is parsing.
        // Phase 1 is source block loading.

        // Phase 2: Prechecks.
        precheck::Precheck::precheck(&ast_schema)?;

        // Phase 3: Lift AST to DML.
        let mut schema = self.lifter.lift(ast_schema)?;

        // Phase 4: Validation
        self.validator.validate(ast_schema, &mut schema)?;

        // TODO: Move consistency stuff into different module.
        // Phase 5: Consistency fixes. These don't fail.
        self.standardiser.standardise(ast_schema, &mut schema)?;

        Ok(schema)
    }
}
