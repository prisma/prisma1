# Parser Prototype for Prisma Datamodel v2

Language: Rust

Build System: Cargo

## Overview

This module is responsible for parsing a string representation of a Prisma datamodel V2, as well as rendering a datamodel to Prisma datamodel V2. The datamodel might include source blocks, so those are loaded as well.

![Architecture Overview](doc/images/overview.png?raw=true)

The validated and standardised **Datamodel** structure is the primary output of this module. For TS/JS applications, a serialized form in DMMF format is provided.

### Design goals

- Strict parsing: A duplicate attribute, unknown attribute, unknown argument or extra argument is an error.
- Ensure consistency of the output.
- Accumulate errors to present them at the end instead of throwing.

### Data Formats

**DMMF** Internal JSON format for transferring datamodel and schema information between different components of Prisma.

**Sources** represents the different data sources (e.g. database connectors) declared in the datamodel.

**Datamodel** is the datamodel data structure which is used by other Rust components of prisma.

**AST** is the internal AST representation of a Prisma datamodel V2 file.

**Datamodel V2 String** is the string representation of a Prisma datamodel V2 file.

### Steps

**Parse** parses a string to an AST and performs basic syntactic checks.

**Load Sources** Loads all source declarations. This injects source-specific attributes into the validation pipeline.

**Lift** converts an AST to a unvalidated datamodel by applying all attributes. Attributes might perform basic validation at this step.

**Validate** performs several checks to ensure the datamodel is valid. This includes, for example, checking invalid type references, or relations which are impossible to model on a database.

**Standardise** sets properties and adds fields and models which are implicitly given. For example. this would add back relation fields, relation tables, and relation's `to_fields`, if they were not explicitly set by the user. The sole purpose of this step is to make working with the generated data structure easier by removing corner cases.

**Convert to DMMF** converts a datamodel (or source) structure to an interal JSON representation used by other non-rust components.

**Convert from DMMF** converts the internal DMMF JSON representation of a datamodel to a datamodel structure.

**Lower** generates an AST from a datamodel. This step will attempt to minimize the AST by removing all attributes and attribute arguments which are a default anyway.

**Render** renders a given AST to it's string representation.

## Guarantees

For a parsed, validated and standardised datamodel, the following guarantees hold:

- Each referred model or enum does exist.
- Each related field has a backwards related field on the related type with equal relation name. If the user did not specify any, a backwards field will be generated.
- All relations are be named.
- All relations have a valid list of `to_fields` on the referencing side. An empty list indicates the back relation field. If the user does not give any `references` argument, the `to_fields` will point to the related types id fields.

**These Guarantees do not hold if a datamodel is loaded from DMMF**

## Usage

Please see [`lib.rs`](src/lib.rs) for all convenience method.

Main use-case, parsing a string to datamodel:

```
let file = fs::read_to_string(&args[1]).expect(&format!("Unable to open file {}", args[1]));

let datamodel = datamodel::parse(&file)?;
```

The `datamodel::errors` module contains a helper method for pretty-printing errors.
