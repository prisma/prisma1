use crate::{ast, common::argument::Arguments, configuration::Generator, errors::*};
use std::collections::HashMap;

pub struct GeneratorLoader {}
impl GeneratorLoader {
    pub fn lift_generator(ast_generator: &ast::GeneratorConfig) -> Result<Generator, ValidationError> {
        let args = Arguments::new(&ast_generator.properties, ast_generator.span);

        let provider = args.arg("provider")?.as_str()?;
        let output = if let Ok(arg) = args.arg("output") {
            Some(arg.as_str()?)
        } else {
            None
        };

        let mut properties: HashMap<String, String> = HashMap::new();

        for prop in &ast_generator.properties {
            // Exclude reserved options.
            if prop.name == "provider" || prop.name == "output" {
                properties.insert(prop.name.clone(), prop.value.to_string());
            }
        }

        Ok(Generator {
            name: ast_generator.name.clone(),
            provider: provider,
            output: output,
            config: properties,
            documentation: ast_generator.documentation.clone().map(|comment| comment.text),
        })
    }

    pub fn lower_generator(generator: &Generator) -> ast::GeneratorConfig {
        let mut arguments: Vec<ast::Argument> = Vec::new();

        arguments.push(ast::Argument::new_string("provider", &generator.provider));
        if let Some(output) = &generator.output {
            arguments.push(ast::Argument::new_string("output", &output));
        }

        for (key, value) in &generator.config {
            arguments.push(ast::Argument::new_string(&key, &value));
        }

        ast::GeneratorConfig {
            name: generator.name.clone(),
            properties: arguments,
            documentation: generator.documentation.clone().map(|text| ast::Comment { text }),
            span: ast::Span::empty(),
        }
    }

    pub fn lift(ast_schema: &ast::Datamodel) -> Result<Vec<Generator>, ErrorCollection> {
        let mut generators: Vec<Generator> = vec![];
        let mut errors = ErrorCollection::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::Top::Generator(gen) => match Self::lift_generator(&gen) {
                    Ok(loaded_gen) => generators.push(loaded_gen),
                    // Lift error.
                    Err(ValidationError::ArgumentNotFound { argument_name, span }) => errors.push(
                        ValidationError::new_generator_argument_not_found_error(&argument_name, &gen.name, &span),
                    ),
                    Err(err) => errors.push(err),
                },
                _ => { /* Non-Source blocks are explicitely ignored by the source loader */ }
            }
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(generators)
        }
    }

    pub fn add_generators_to_ast(generators: &Vec<Generator>, ast_datamodel: &mut ast::Datamodel) {
        let mut models: Vec<ast::Top> = Vec::new();

        for generator in generators {
            models.push(ast::Top::Generator(Self::lower_generator(&generator)))
        }

        // Prepend generstors.
        models.append(&mut ast_datamodel.models);

        ast_datamodel.models = models;
    }
}
