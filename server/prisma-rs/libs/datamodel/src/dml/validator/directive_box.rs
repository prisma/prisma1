use super::directive::core::{new_builtin_enum_directives, new_builtin_field_directives, new_builtin_model_directives};
use super::directive::DirectiveListValidator;
use crate::{dml, source};

pub struct DirectiveBox {
    pub field: DirectiveListValidator<dml::Field>,
    pub model: DirectiveListValidator<dml::Model>,
    pub enm: DirectiveListValidator<dml::Enum>,
}

impl DirectiveBox {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> DirectiveBox {
        DirectiveBox {
            field: new_builtin_field_directives(),
            model: new_builtin_model_directives(),
            enm: new_builtin_enum_directives(),
        }
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &Vec<Box<source::Source>>) -> DirectiveBox {
        let mut directives = DirectiveBox::new();

        for source in sources {
            directives
                .enm
                .add_all_scoped(source.get_enum_directives(), source.name());
            directives
                .field
                .add_all_scoped(source.get_field_directives(), source.name());
            directives
                .model
                .add_all_scoped(source.get_model_directives(), source.name());
        }

        return directives;
    }
}
