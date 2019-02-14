import { printSchema } from 'graphql/utilities'
import { GraphQLSchema } from 'graphql/type'
import { IGQLType, Parser, DatabaseType, ISDL } from 'prisma-datamodel'
import Generator from './generator'

/**
 * Computes the internal type representation for a model.
 * @param model The model in SDL as string.
 * @param databaseType: The database type implementation to use.
 * @returns An array of all types present in the model.
 */
export function parseInternalTypes(
  model: string,
  databaseType: DatabaseType,
): ISDL {
  return Parser.create(databaseType).parseFromSchemaString(model)
}

/**
 * Computes a prisma OpenCRUD schema for a given model.
 * @param model The model in SDL as string.
 * @param databaseType: The database type implementation to use.
 * @returns The OpenCRUD schema as graphql-js schema object for the given model.
 */
export function generateCRUDSchema(
  model: string,
  databaseType: DatabaseType,
): GraphQLSchema {
  const { types } = parseInternalTypes(model, databaseType)
  return Generator.create(databaseType).schema.generate(types.sort(
    ({ name: a }, { name: b }) => (a > b ? 1 : -1),
  ), {})
}

/**
 * Computes a prisma OpenCRUD schema for a given model.
 * @param model The model in SDL as string.
 * @param databaseType: The database type implementation to use.
 * @returns The OpenCRUD schema as prettified string for the given model.
 */
export default function generateCRUDSchemaString(
  model: string,
  databaseType: DatabaseType = DatabaseType.postgres,
): string {
  return printSchema(generateCRUDSchema(model, databaseType))
}