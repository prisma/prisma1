use crate::{ast, common::argument::Arguments, common::value::ValueListValidator, configuration::Generator, errors::*};
use std::collections::HashMap;

pub struct GeneratorLoader {}

const PROVIDER_KEY: &str = "provider";
const OUTPUT_KEY: &str = "output";
const PLATFORMS_KEY: &str = "platforms";
const PINNED_PLATFORM_KEY: &str = "pinnedPlatform";
const FIRST_CLASS_PROPERTIES: &'static [&'static str] = &[PROVIDER_KEY, OUTPUT_KEY, PLATFORMS_KEY, PINNED_PLATFORM_KEY];

impl GeneratorLoader {
    pub fn lift_generator(ast_generator: &ast::GeneratorConfig) -> Result<Generator, ValidationError> {
        let mut args = Arguments::new(&ast_generator.properties, ast_generator.span);

        let provider = args.arg(PROVIDER_KEY)?.as_str()?;
        let output = if let Ok(arg) = args.arg(OUTPUT_KEY) {
            Some(arg.as_str()?)
        } else {
            None
        };

        let mut properties: HashMap<String, String> = HashMap::new();

        let platforms = match args.arg(PLATFORMS_KEY).ok() {
            Some(x) => x.as_array()?.to_str_vec()?,
            None => Vec::new(),
        };
        let pinned_platform = args.arg(PINNED_PLATFORM_KEY).and_then(|x| x.as_str()).ok();

        for prop in &ast_generator.properties {
            let is_first_class_prop = FIRST_CLASS_PROPERTIES.iter().find(|k| *k == &prop.name.name).is_some();
            if is_first_class_prop {
                continue;
            }

            properties.insert(prop.name.name.clone(), prop.value.to_string());
        }

        Ok(Generator {
            name: ast_generator.name.name.clone(),
            provider: provider,
            output: output,
            platforms: platforms,
            pinned_platform: pinned_platform,
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

        let platform_values: Vec<ast::Value> = generator
            .platforms
            .iter()
            .map(|p| ast::Value::StringValue(p.to_string(), ast::Span::empty()))
            .collect();
        if !platform_values.is_empty() {
            arguments.push(ast::Argument::new_array("platforms", platform_values));
        }

        if let Some(pinned_platform) = &generator.pinned_platform {
            arguments.push(ast::Argument::new_string("pinnedPlatform", &pinned_platform));
        }

        for (key, value) in &generator.config {
            arguments.push(ast::Argument::new_string(&key, &value));
        }

        ast::GeneratorConfig {
            name: ast::Identifier::new(&generator.name),
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
                        ValidationError::new_generator_argument_not_found_error(&argument_name, &gen.name.name, &span),
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
