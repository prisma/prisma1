use super::traits::{Source, SourceDefinition};
use crate::ast;
use crate::common::argument::Arguments;
use crate::errors::{ErrorCollection, ValidationError};
use crate::StringFromEnvVar;

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
    pub fn load_source(&self, ast_source: &ast::SourceConfig) -> Result<Option<Box<Source>>, ValidationError> {
        let mut args = Arguments::new(&ast_source.properties, ast_source.span);
        let (env_var_for_url, url) = args.arg("url")?.as_str_from_env()?;
        let provider_arg = args.arg("provider")?;
        let provider = provider_arg.as_str()?;

        if let Ok(arg) = args.arg("enabled") {
            if false == arg.as_bool()? {
                // This source was disabled.
                return Ok(None);
            }
        }

        for decl in &self.source_declarations {
            // The provider given in the config block identifies the source type.
            // TODO: The second condition is a fallback to mitigate the postgres -> postgresql rename. It should be
            // renamed at some point.
            if provider == decl.connector_type() || (decl.connector_type() == "postgresql" && provider == "postgres") {
                return Ok(Some(decl.create(
                    // The name in front of the block is the name of the concrete instantiation.
                    &ast_source.name.name,
                    StringFromEnvVar {
                        from_env_var: env_var_for_url,
                        value: url,
                    },
                    &mut Arguments::new(&ast_source.properties, ast_source.span),
                    &ast_source.documentation.clone().map(|comment| comment.text),
                )?));
            }
        }

        Err(ValidationError::new_source_not_known_error(
            &provider,
            &provider_arg.span(),
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
                    Ok(Some(loaded_src)) => sources.push(loaded_src),
                    Ok(None) => { /* Source was disabled. */ }
                    // Lift error to source.
                    Err(ValidationError::ArgumentNotFound { argument_name, span }) => errors.push(
                        ValidationError::new_source_argument_not_found_error(&argument_name, &src.name.name, &span),
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
