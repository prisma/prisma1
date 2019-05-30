use super::traits::{Source, SourceDefinition};
use crate::ast;
use crate::common::argument::Arguments;
use crate::errors::{ErrorCollection, ValidationError};

/// Helper struct to load and validate source configuration blocks.
pub struct SourceLoader {
    source_declarations: Vec<Box<SourceDefinition>>,
}

impl SourceLoader {
    /// Creates a new, empty source loader.
    pub fn new() -> SourceLoader {
        SourceLoader {
            source_declarations: vec![],
        }
    }

    /// Adds a source definition to this loader.
    pub fn add_source_definition(&mut self, source_definition: Box<SourceDefinition>) {
        self.source_declarations.push(source_definition);
    }

    /// Internal: Loads a single source from a source config block in the datamodel.
    fn load_source(&self, ast_source: &ast::SourceConfig) -> Result<Box<Source>, ValidationError> {
        let args = Arguments::new(&ast_source.properties, ast_source.span);
        let url = args.arg("url")?.as_str()?;
        let name = args.arg("name")?.as_str()?;

        for decl in &self.source_declarations {
            // The name given in the config block identifies the source type.
            if name == decl.name() {
                return decl.create(
                    // The name in front of the block is the name of the concrete instantiation.
                    &ast_source.name,
                    &url,
                    &Arguments::new(&ast_source.detail_configuration, ast_source.span),
                );
            }
        }

        Err(ValidationError::new_source_not_known_error(
            &ast_source.name,
            &ast_source.span,
        ))
    }

    /// Loads all source config blocks form the given AST,
    /// and returns a Source instance for each.
    pub fn load(&self, ast_schema: &ast::Datamodel) -> Result<Vec<Box<Source>>, ErrorCollection> {
        let mut sources: Vec<Box<Source>> = vec![];
        let mut errors = ErrorCollection::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::Top::Source(src) => match self.load_source(&src) {
                    Ok(loaded_src) => sources.push(loaded_src),
                    // Lift error to source.
                    Err(ValidationError::ArgumentNotFound { argument_name, span }) => errors.push(
                        ValidationError::new_source_argument_not_found_error(&argument_name, &src.name, &span),
                    ),
                    Err(err) => errors.push(err),
                },
                _ => { /* Non-Source blocks are explicitely ignored by the source loader */ }
            }
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(sources)
        }
    }
}
