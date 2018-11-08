import Generators from './generator/defaultGenerators'
import DatamodelParser from './datamodel/parser'
import { printSchema } from 'graphql/utilities'
import { GraphQLSchema } from 'graphql/type'
import { IGQLType } from './datamodel/model'

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
export function generateCRUDSchema(model: string): GraphQLSchema {
  const generators = new Generators()
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
export default function generateCRUDSchemaString(model: string): string {
  return printSchema(generateCRUDSchema(model))
}
