pub struct QuerySchema {
    query: ObjectType,    // read(s)?
    mutation: ObjectType, // write(s)?
}

// enum for Optional input types, list types?
// Could also be a flag on the structs

impl QuerySchema {}

struct ObjectType {}

// On schema construction checks:
// - field name uniqueness
// - val NameRegexp = """^[_a-zA-Z][_a-zA-Z0-9]*$""".r match
// -

enum InputType {
    EnumType,
    InputObjectType,
    ListInputType,
    OptionInputType,
    ScalarType,
}

enum OutputType {
    EnumType,
    ListType(OutputType),
    ObjectType(ObjectType),
    OptionType(OutputType),
    ScalarType,
}

// Possible:
// InputType(OptionType(StringType))
