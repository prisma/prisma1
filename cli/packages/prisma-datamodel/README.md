# prisma-datamodel

The Prisma Datamodel package forms the foundation of all datamodel related tasks in the CLI.

### Components

* Data structures to represent datamodels in memory: `ISDL`, `IGQLType`, `IGQLField`. These data structures are documented inline. The data structures might be self referencing, and all operations in this library guarantee to keep the references valid.
* Constants for known primitive types: `TypeIdentifier`, `TypeIdentifiers`
* Classes to parse data models from strings into the internal format: `Parser`, with the factory class`DefaultParser`
* Classes to render data models to strings, from the internal format: `Renderer`, with the factory class `Default Renderer`
* Auxiliary functions: `cloneSchema` to safely clone an `ISDL`structure, `toposort`to sort a datamodel in topological order.

### Different Database Types

When creating a parser or renderer, a flag that indicates the database type has to be passed. The internal representation is guaranteed to be consistent between different databases. It is possible to parse a mongo schema and render a postgres schema without any transformations in between.

### Datamodel V1 vs. V1.1

The parser is capable of parsing both datamodel formats, and even models with mixed directives from both standards. For rendering, a flag can be passed which indicates the datamodel format to follow. 

### Modifying a Model

The types `ISDL`, `IGQLType` and `IGQLField` are designed to allow convenient analysis and transformation. Most notably, they may contain circular references (for representing related types and indexes). Therefor, these types are **mutable**, and care has to be taken when modifying them, for example by cloning them using `cloneSchema`.

When adding or removing a type, it is important to also update all referring fields or indexes, otherwise other transformations or the rendering process might break.

### Usage

Basic example:

```typescript
const parser = DefaultParser.create(DatabaseType.mongo)
const model = parser.parse(datamodelAsString)

// Do something with the model
for(const type of model.types) {
    console.log(`${type.name} has ${type.fields.length} fields and ${type.indices.length} indexes`)
}

const enableDatamodel1_1 = true
const renderer = DefaultRenderer.create(DatabaseType.postgres, enableDatamodel1_1)

const renderedAsString = renderer.render(model)
```

