use super::traits::Source;
use crate::ast;

pub struct SourceSerializer {}

impl SourceSerializer {
    pub fn source_to_ast(source: &Box<Source>) -> ast::SourceConfig {
        let mut arguments: Vec<ast::Argument> = Vec::new();

        arguments.push(ast::Argument::new_string("provider", source.connector_type()));
        arguments.push(ast::Argument::new_string("url", source.url()));

        let mut detail_arguments: Vec<ast::Argument> = Vec::new();

        for (key, value) in &source.config() {
            detail_arguments.push(ast::Argument::new_string(&key, &value));
        }

        ast::SourceConfig {
            name: source.name().clone(),
            properties: arguments,
            detail_configuration: detail_arguments,
            documentation: source.documentation().clone().map(|text| ast::Comment { text }),
            span: ast::Span::empty(),
        }
    }

    pub fn add_sources_to_ast(sources: &Vec<Box<Source>>, ast_datamodel: &mut ast::Datamodel) {
        let mut models: Vec<ast::Top> = Vec::new();

        for source in sources {
            models.push(ast::Top::Source(Self::source_to_ast(&source)))
        }

        // Prepend sources.
        models.append(&mut ast_datamodel.models);

        ast_datamodel.models = models;
    }
}
