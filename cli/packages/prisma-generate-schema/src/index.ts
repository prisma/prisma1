import RelationalGenerator from './generator/default/'
import DocumentGenerator from './generator/document/'
import DatamodelParser from './datamodel/parser'
import { printSchema } from 'graphql/utilities'
import { GraphQLSchema } from 'graphql/type'
import { IGQLType } from './datamodel/model'

// Please replace this type as needed.
export enum DatabaseType {
  document = 'document',
  relational = 'relational'
}

/**
 * Computes the internal type representation for a model.
 * @param model The model in SDL as string.
 * @returns An array of all types present in the model.
 */
export function parseInternalTypes(model: string): IGQLType[] {
  return DatamodelParser.parseFromSchemaString(model)
}

/**
 * Computes a prisma OpenCRUD schema for a given model.
 * @param model The model in SDL as string.
 * @returns The OpenCRUD schema as graphql-js schema object for the given model.
 */
export function generateCRUDSchema(model: string, databaseType: DatabaseType = DatabaseType.relational): GraphQLSchema {
  const generators = databaseType === DatabaseType.document ? new DocumentGenerator() : new RelationalGenerator()
  const types = parseInternalTypes(model).sort(({ name: a }, { name: b }) =>
    a > b ? 1 : -1,
  )

  return generators.schema.generate(types, {})
}

/**
 * Computes a prisma OpenCRUD schema for a given model.
 * @param model The model in SDL as string.
 * @returns The OpenCRUD schema as prettified string for the given model.
 */
export default function generateCRUDSchemaString(model: string, databaseType: DatabaseType = DatabaseType.relational): string {
  return printSchema(generateCRUDSchema(model, databaseType))
}
