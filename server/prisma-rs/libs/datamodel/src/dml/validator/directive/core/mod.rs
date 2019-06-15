use crate::dml;
use crate::validator::directive::DirectiveListValidator;
use std::collections::BTreeMap;

mod default;
mod embedded;
mod id;
mod map;
mod relation;
mod scalarlist;
mod sequence;
mod unique;
mod updated_at;

/// Returns a directive list validator containing all builtin field directives.
pub fn new_builtin_field_directives() -> DirectiveListValidator<dml::Field> {
    let mut validator = DirectiveListValidator::<dml::Field> {
        known_directives: BTreeMap::new(),
    };

    validator.add(Box::new(map::MapDirectiveValidator {}));
    validator.add(Box::new(id::IdDirectiveValidator {}));
    validator.add(Box::new(scalarlist::ScalarListDirectiveValidator {}));
    validator.add(Box::new(sequence::SequenceDirectiveValidator {}));
    validator.add(Box::new(unique::UniqueDirectiveValidator {}));
    validator.add(Box::new(default::DefaultDirectiveValidator {}));
    validator.add(Box::new(relation::RelationDirectiveValidator {}));
    validator.add(Box::new(updated_at::UpdatedAtDirectiveValidator {}));

    return validator;
}

/// Returns a directive list validator containing all builtin model directives.
pub fn new_builtin_model_directives() -> DirectiveListValidator<dml::Model> {
    let mut validator = DirectiveListValidator::<dml::Model> {
        known_directives: BTreeMap::new(),
    };

    validator.add(Box::new(map::MapDirectiveValidator {}));
    validator.add(Box::new(embedded::EmbeddedDirectiveValidator {}));

    return validator;
}

/// Returns a directive list validator containing all builtin enum directives.
pub fn new_builtin_enum_directives() -> DirectiveListValidator<dml::Enum> {
    let validator = DirectiveListValidator::<dml::Enum> {
        known_directives: BTreeMap::new(),
    };

    // No enum derictives yet.

    return validator;
}
