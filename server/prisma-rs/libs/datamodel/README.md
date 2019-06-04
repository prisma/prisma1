# Parser Prototype for Prisma Datamodel v2

Language: Rust

Build System: Cargo

### Design goals

- Strict parsing: A duplicate directive, unknown directive, unknown argument or extra argument is an error.
- Accumulate errors to present them at the end instead of throwing

### Guarantees

For a parsed and validated datmodel, the following guarantees hold:

- Each referred model or enum does exist.
- Each related field has a backwards related field on the related type with equal relation name. If the user did not specify any, a backwards field will be generated.
- All relations are be named.
- All relations have a valid list of `to_fields` on the referencing side. An empty list indicates the back relation field. If the user does not give any `references` argument, the `to_fields` will point to the related types id fields.

### Usage

```
let file = fs::read_to_string(&args[1]).expect(&format!("Unable to open file {}", args[1]));

let ast = parser::parse(&file);
let validator = Validator::new();
let dml = validator.validate(&ast);
```

### Error Handling

Currently, we panic on the first error. This will change in the future, and `Validator::validate` will return a proper `Result` object.
