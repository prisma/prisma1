import { printSchema } from 'graphql/utilities'
import { GraphQLSchema } from 'graphql/type'
import { IGQLType, DefaultParser, DatabaseType, ISDL } from 'prisma-datamodel'
import Generator from './generator'

/**
 * Schema generator factory for different database types.
 */
export { default as CRUDSchemaGenerator } from './generator'

/**
 * Computes the internal type representation for a model.
 * @param model The model in SDL as string.
 * @param databaseType: The database type implementation to use.
 * @returns An ISDL object containing all types present in the model.
 */
export function parseInternalTypes(
  model: string,
  databaseType: DatabaseType,
): ISDL {
  return DefaultParser.create(databaseType).parseFromSchemaString(model)
}

/**
 * Computes a prisma prisma client CRUD schema for a given model.
 * @param model The model in SDL as string.
 * @param databaseType: The database type implementation to use.
 * @returns The prisma client CRUD schema as graphql-js schema object for the given model.
 */
export function generateCRUDSchema(
  model: string,
  databaseType: DatabaseType,
): GraphQLSchema {
  const { types } = parseInternalTypes(model, databaseType)
  return Generator.create(databaseType).schema.generate(
    types.sort(({ name: a }, { name: b }) => (a > b ? 1 : -1)),
    {},
  )
}

/**
 * Creates a prisma client CRUD schema from a given model.
 * @param model The model as internal type datastructure (ISDL)
 * @param databaseType The database type to generate the schema for.
 * @returns The prisma client CRUD schema as graphql-js schema object for the given model.
 */
export function generateCRUDSchemaFromInternalISDL(
  model: ISDL,
  databaseType: DatabaseType,
): GraphQLSchema {
  return Generator.create(databaseType).schema.generate(model.types, {})
}

/**
 * Computes a prisma prisma client CRUD schema for a given model.
 * @param model The model in SDL as string.
 * @param databaseType: The database type implementation to use.
 * @returns The prisma client CRUD schema as prettified string for the given model.
 */
export default function generateCRUDSchemaString(
  model: string,
  databaseType: DatabaseType = DatabaseType.postgres,
): string {
  return printSchema(generateCRUDSchema(model, databaseType))
}
