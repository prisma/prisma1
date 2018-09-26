# prisma-generate-schema

This module is capable of generating a prisma [OpenCRUD](https://www.opencrud.org/) schema for a given datamodel, given in SDL.

## Developing

Development uses typescript, npm, yarn and jest.

```
yarn
npm run test
```

## Usage

Basic Usage:

```typescript
import { generateCRUDSchemaString } from 'prisma-generate-schema'

const openCRUDSchema = generateCRUDSchemaString(modelInSDL)
```

## Project Structure

This section is intended for maintainers.

First, a datamodel is parsed using the [`DatamodelParser`](src/datamodel/parser.ts). Then, an OpenCRUD schema is created using the [`SchemaGenerator`](src/generator/schemaGenerator.ts).

The schema generator utilizes several other generators to generate the `mutation`, `query` and `subscription` objects and all corresponding types. A [`Generator`](src/generator/generator.ts) is usually responsible for a single type only but will access other generators to recursively build the schema. All generators which return object types implement lazy evaluation and caching of generated types via their base class. The generators can be configured using dependency injection, if needed, to switch out the implementation for certain types in the schema. All default generators, one for each type that can occur in an OpenCRUD schema, can ge found in the [`DefaultGenerators`](src/generator/defaultGenerators.ts) class.

