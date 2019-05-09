# Parser Prototype for Prisma Datamodel v2

Language: Rust

Build System: Cargo

**Please consider this a WIP prototype. API's might change.**

### Design goals

* Strict parsing: A duplicate directive, unknown directive, unknown argument or extra argument is an error.
* Accumulate errors to present them at the end instead of throwing (TODO)

### Usage

```
let file = fs::read_to_string(&args[1]).expect(&format!("Unable to open file {}", args[1]));

let ast = parser::parse(&file);
let validator = Validator::new();
let dml = validator.validate(&ast);
```

### Error Handling

Currently, we panic on the first error. This will change in the future, and `Validator::validate` will return a proper `Result` object.