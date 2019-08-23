mod collection;
pub use collection::*;

use crate::ast::Span;
use colored::Colorize;

// No format for this file, on purpose.
// Line breaks make the declarations very hard to read.
#[rustfmt::skip]
/// Enum for different errors which can happen during
/// parsing or validation.
///
/// For fancy printing, please use the `pretty_print_error` function.
#[derive(Debug, Fail, Clone, PartialEq)]
pub enum ValidationError {
    #[fail(display = "Argument \"{}\" is missing.", argument_name)]
    ArgumentNotFound { argument_name: String, span: Span },

    #[fail(display = "Function \"{}\" takes {} arguments, but received {}.", function_name, required_count, given_count)]
    ArgumentCountMissmatch { function_name: String, required_count: usize, given_count: usize, span: Span },

    #[fail(display = "Argument \"{}\" is missing in attribute \"@{}\".", argument_name, directive_name)]
    DirectiveArgumentNotFound { argument_name: String, directive_name: String, span: Span },

    #[fail(display = "Argument \"{}\" is missing in data source block \"{}\".", argument_name, source_name)]
    SourceArgumentNotFound { argument_name: String, source_name: String, span: Span },

    #[fail(display = "Argument \"{}\" is missing in generator block \"{}\".", argument_name, generator_name)]
    GeneratorArgumentNotFound { argument_name: String, generator_name: String, span: Span },

    #[fail(display = "Error parsing attribute \"@{}\": {}.", directive_name, message)]
    DirectiveValidationError { message: String, directive_name: String, span: Span },

    #[fail(display = "Attribute \"@{}\" is defined twice.", directive_name)]
    DuplicateDirectiveError { directive_name: String, span: Span },

    #[fail(display = "\"{}\" is a reserved scalar type name and can not be used.", type_name)]
    ReservedScalarTypeError { type_name: String, span: Span },

    #[fail(display = "The {} \"{}\" cannot be defined because a {} with that name already exists.", top_type, name, existing_top_type)]
    DuplicateTopError { name: String, top_type: String, existing_top_type: String, span: Span },

    // conf_block_name is pre-populated with "" in precheck.ts.
    #[fail(display = "Key \"{}\" is already defined in {}.", key_name, conf_block_name)]
    DuplicateConfigKeyError { conf_block_name: String, key_name: String, span: Span },

    #[fail(display = "Argument \"{}\" is already specified as unnamed argument.", arg_name)]
    DuplicateDefaultArgumentError { arg_name: String, span: Span },

    #[fail(display = "Argument \"{}\" is already specified.", arg_name)]
    DuplicateArgumentError { arg_name: String, span: Span },

    #[fail(display = "No such argument.")]
    UnusedArgumentError { arg_name: String, span: Span },

    #[fail(display = "Field \"{}\" is already defined on model \"{}\".", field_name, model_name)]
    DuplicateFieldError { model_name: String, field_name: String, span: Span },

    #[fail(display = "Value \"{}\" is already defined on enum \"{}\".", value_name, enum_name)]
    DuplicateEnumValueError { enum_name: String, value_name: String, span: Span },

    #[fail(display = "Attribute not known: \"@{}\".", directive_name)]
    DirectiveNotKnownError { directive_name: String, span: Span },

    #[fail(display = "Function not known: \"{}\".", function_name)]
    FunctionNotKnownError { function_name: String, span: Span },

    #[fail(display = "Datasource provider not known: \"{}\".", source_name)]
    SourceNotKnownError { source_name: String, span: Span },

    #[fail(display = "\"{}\" is not a valid value for {}.", raw_value, literal_type)]
    LiteralParseError { literal_type: String, raw_value: String, span: Span },

    #[fail(display = "Type \"{}\" is neither a built-in type, nor refers to another model, custom type, or enum.", type_name)]
    TypeNotFoundError { type_name: String, span: Span },

    #[fail(display = "Type \"{}\" is not a built-in type.", type_name)]
    ScalarTypeNotFoundError { type_name: String, span: Span },

    #[fail(display = "Unexpected token. Expected one of: {}.", expected_str)]
    ParserError { expected: Vec<&'static str>, expected_str: String, span: Span },

    #[fail(display = "{}", message)]
    LegacyParserError { message: String, span: Span },

    #[fail(display = "{}", message)]
    FunctionalEvaluationError { message: String, span: Span },

    #[fail(display = "Environment variable not found: {}.", var_name)]
    EnvironmentFunctionalEvaluationError { var_name: String, span: Span },

    #[fail(display = "Expected a {} value, but received {} value \"{}\".", expected_type, received_type, raw)]
    TypeMismatchError { expected_type: String, received_type: String, raw: String, span: Span },

    #[fail(display = "Expected a {} value, but failed while parsing \"{}\": {}.", expected_type, raw, parser_error)]
    ValueParserError { expected_type: String, parser_error: String, raw: String, span: Span },

    #[fail(display = "Error validating model \"{}\": {}.", model_name, message)]
    ModelValidationError { message: String, model_name: String, span: Span  },

    #[fail(display = "Error validating: {}.", message)]
    ValidationError { message: String, span: Span  },
}

#[rustfmt::skip]
impl ValidationError {
    pub fn new_literal_parser_error(literal_type: &str, raw_value: &str, span: Span) -> ValidationError {
        ValidationError::LiteralParseError {
            literal_type: String::from(literal_type),
            raw_value: String::from(raw_value),
            span,
        }
    }

    pub fn new_argument_not_found_error(argument_name: &str, span: Span) -> ValidationError {
        ValidationError::ArgumentNotFound { argument_name: String::from(argument_name), span }
    }

    pub fn new_argument_count_missmatch_error(function_name: &str, required_count: usize, given_count: usize, span: Span) -> ValidationError {
        ValidationError::ArgumentCountMissmatch {
            function_name: String::from(function_name),
            required_count,
            given_count,
            span,
        }
    }

    pub fn new_directive_argument_not_found_error(argument_name: &str, directive_name: &str, span: Span) -> ValidationError {
        ValidationError::DirectiveArgumentNotFound {
            argument_name: String::from(argument_name),
            directive_name: String::from(directive_name),
            span,
        }
    }

    pub fn new_source_argument_not_found_error(argument_name: &str, source_name: &str, span: Span) -> ValidationError {
        ValidationError::SourceArgumentNotFound {
            argument_name: String::from(argument_name),
            source_name: String::from(source_name),
            span,
        }
    }

    pub fn new_generator_argument_not_found_error(argument_name: &str, generator_name: &str, span: Span) -> ValidationError {
        ValidationError::GeneratorArgumentNotFound {
            argument_name: String::from(argument_name),
            generator_name: String::from(generator_name),
            span,
        }
    }

    pub fn new_directive_validation_error(message: &str, directive_name: &str, span: Span) -> ValidationError {
        ValidationError::DirectiveValidationError {
            message: String::from(message),
            directive_name: String::from(directive_name),
            span,
        }
    }

    pub fn new_duplicate_directive_error(directive_name: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateDirectiveError {
            directive_name: String::from(directive_name),
            span,
        }
    }

    pub fn new_reserved_scalar_type_error(type_name: &str, span: Span) -> ValidationError {
        ValidationError::ReservedScalarTypeError {
            type_name: String::from(type_name),
            span,
        }
    }

    pub fn new_duplicate_top_error(name: &str, top_type: &str, existing_top_type: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateTopError {
            name: String::from(name),
            top_type: String::from(top_type),
            existing_top_type: String::from(existing_top_type),
            span,
        }
    }

    pub fn new_duplicate_config_key_error(conf_block_name: &str, key_name: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateConfigKeyError {
            conf_block_name: String::from(conf_block_name),
            key_name: String::from(key_name),
            span,
        }
    }

    pub fn new_duplicate_argument_error(arg_name: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateArgumentError {
            arg_name: String::from(arg_name),
            span,
        }
    }

    pub fn new_unused_argument_error(arg_name: &str, span: Span) -> ValidationError {
        ValidationError::UnusedArgumentError {
            arg_name: String::from(arg_name),
            span,
        }
    }

    pub fn new_duplicate_default_argument_error(arg_name: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateDefaultArgumentError {
            arg_name: String::from(arg_name),
            span,
        }
    }

    pub fn new_duplicate_enum_value_error(enum_name: &str, value_name: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateEnumValueError {
            enum_name: String::from(enum_name),
            value_name: String::from(value_name),
            span,
        }
    }

    pub fn new_duplicate_field_error(model_name: &str, field_name: &str, span: Span) -> ValidationError {
        ValidationError::DuplicateFieldError {
            model_name: String::from(model_name),
            field_name: String::from(field_name),
            span,
        }
    }


    pub fn new_model_validation_error(message: &str, model_name: &str, span: Span) -> ValidationError {
        ValidationError::ModelValidationError {
            message: String::from(message),
            model_name: String::from(model_name),
            span,
        }
    }

    pub fn new_validation_error(message: &str, span: Span) -> ValidationError {
        ValidationError::ValidationError {
            message: String::from(message),
            span,
        }
    }

    pub fn new_legacy_parser_error(message: &str, span: Span) -> ValidationError {
        ValidationError::LegacyParserError {
            message: String::from(message),
            span,
        }
    }

    pub fn new_parser_error(expected: &Vec<&'static str>, span: Span) -> ValidationError {
        ValidationError::ParserError { expected: expected.clone(), expected_str: expected.join(", "), span }
    }
    pub fn new_functional_evaluation_error(message: &str, span: Span) -> ValidationError {
        ValidationError::FunctionalEvaluationError { message: String::from(message), span }
    }
    pub fn new_environment_functional_evaluation_error(var_name: &str, span: Span) -> ValidationError {
        ValidationError::EnvironmentFunctionalEvaluationError { var_name: String::from(var_name), span }
    }
    pub fn new_type_not_found_error(type_name: &str, span: Span) -> ValidationError {
        ValidationError::TypeNotFoundError { type_name: String::from(type_name), span }
    }
    pub fn new_scalar_type_not_found_error(type_name: &str, span: Span) -> ValidationError {
        ValidationError::ScalarTypeNotFoundError { type_name: String::from(type_name), span }
    }
    pub fn new_directive_not_known_error(directive_name: &str, span: Span) -> ValidationError {
        ValidationError::DirectiveNotKnownError { directive_name: String::from(directive_name), span }
    }
    pub fn new_function_not_known_error(function_name: &str, span: Span) -> ValidationError {
        ValidationError::FunctionNotKnownError { function_name: String::from(function_name), span }
    }

    pub fn new_source_not_known_error(source_name: &str, span: Span) -> ValidationError {
        ValidationError::SourceNotKnownError { source_name: String::from(source_name), span }
    }

    pub fn new_value_parser_error(expected_type: &str, parser_error: &str, raw: &str, span: Span) -> ValidationError {
        ValidationError::ValueParserError {
            expected_type: String::from(expected_type),
            parser_error: String::from(parser_error),
            raw: String::from(raw),
            span,
        }
    }

    pub fn new_type_mismatch_error(expected_type: &str, received_type: &str, raw: &str, span: Span) -> ValidationError {
        ValidationError::TypeMismatchError {
            expected_type: String::from(expected_type),
            received_type: String::from(received_type),
            raw: String::from(raw),
            span,
        }
    }

    pub fn span(&self) -> Span {
        match self {
            ValidationError::ArgumentNotFound { span, .. } => *span,
            ValidationError::DirectiveArgumentNotFound { span, .. } => *span,
            ValidationError::ArgumentCountMissmatch { span, .. } => *span,
            ValidationError::SourceArgumentNotFound { span, .. } => *span,
            ValidationError::GeneratorArgumentNotFound { span, .. } => *span,
            ValidationError::DirectiveValidationError { span, .. } => *span,
            ValidationError::DirectiveNotKnownError { span, .. } => *span,
            ValidationError::ReservedScalarTypeError { span, .. } => *span,
            ValidationError::FunctionNotKnownError { span, .. } => *span,
            ValidationError::SourceNotKnownError { span, .. } => *span,
            ValidationError::LiteralParseError { span, .. } => *span,
            ValidationError::TypeNotFoundError { span, .. } => *span,
            ValidationError::ScalarTypeNotFoundError { span, .. } => *span,
            ValidationError::ParserError { span, .. } => *span,
            ValidationError::FunctionalEvaluationError { span, .. } => *span,
            ValidationError::EnvironmentFunctionalEvaluationError { span, .. } => *span,
            ValidationError::TypeMismatchError { span, .. } => *span,
            ValidationError::ValueParserError { span, .. } => *span,
            ValidationError::ValidationError { span, .. } => *span,
            ValidationError::LegacyParserError { span, .. } => *span,
            ValidationError::ModelValidationError { span, .. } => *span,
            ValidationError::DuplicateDirectiveError { span, .. } => *span,
            ValidationError::DuplicateConfigKeyError { span, .. } => *span,
            ValidationError::DuplicateTopError { span, .. } => *span,
            ValidationError::DuplicateFieldError { span, .. } => *span,
            ValidationError::DuplicateEnumValueError { span, .. } => *span,
            ValidationError::DuplicateArgumentError { span, .. } => *span,
            ValidationError::DuplicateDefaultArgumentError { span, .. } => *span,
            ValidationError::UnusedArgumentError { span, .. } => *span
        }
    }
    pub fn description(&self) -> String {
        format!("{}", self)
    }

    pub fn pretty_print(&self, f: &mut dyn std::io::Write, file_name: &str, text: &str) -> std::io::Result<()> {
        pretty_print_error(f, file_name, text, self)
    }
}

/// Given the datamodel text representation, pretty prints an error, including
/// the offending portion of the source code, for human-friendly reading.
#[rustfmt::skip]
pub fn pretty_print_error(f: &mut dyn std::io::Write, file_name: &str, text: &str, error_obj: &ValidationError) -> std::io::Result<()> {
    let span = error_obj.span();
    let error = error_obj.description();

    let line_number = text[..span.start].matches("\n").count();
    let file_lines = text.split("\n").collect::<Vec<&str>>();

    let chars_in_line_before: usize = file_lines[..line_number].iter().map(|l| l.len()).sum();
    // Don't forget to count the all the line breaks.
    let chars_in_line_before = chars_in_line_before + line_number;

    let line = &file_lines[line_number];

    let start_in_line = span.start - chars_in_line_before;
    let end_in_line = std::cmp::min(start_in_line + (span.end - span.start), line.len());

    let prefix = &line[..start_in_line];
    let offending = &line[start_in_line..end_in_line].bright_red().bold();
    let suffix = &line[end_in_line..];

    let arrow = "-->".bright_blue().bold();
    let file_path = format!("{}:{}", file_name, line_number + 1).underline();

    writeln!(f, "{}: {}", "error".bright_red().bold(), error.bold())?;
    writeln!(f, "  {}  {}", arrow, file_path)?;
    writeln!(f, "{}", format_line_number(0))?;
    writeln!(f, "{}", format_line_number_with_line(line_number, &file_lines))?;
    writeln!(f, "{}{}{}{}", format_line_number(line_number + 1), prefix, offending, suffix)?;
    if offending.len() == 0 {
        let spacing = std::iter::repeat(" ").take(start_in_line).collect::<String>();
        writeln!(f, "{}{}{}", format_line_number(0), spacing, "^ Unexpected token.".bold().bright_red())?;
    }
    writeln!(f, "{}", format_line_number_with_line(line_number + 2, &file_lines))?;
    writeln!(f, "{}", format_line_number(0))
}

fn format_line_number_with_line(line_number: usize, lines: &Vec<&str>) -> colored::ColoredString {
    if line_number > 0 && line_number <= lines.len() {
        colored::ColoredString::from(format!("{}{}", format_line_number(line_number), lines[line_number - 1]).as_str())
    } else {
        format_line_number(line_number)
    }
}
fn format_line_number(line_number: usize) -> colored::ColoredString {
    if line_number > 0 {
        format!("{:2} | ", line_number).bold().bright_blue()
    } else {
        "   | ".bold().bright_blue()
    }
}
