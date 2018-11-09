import { IGQLType, IGQLField, GQLScalarField } from './model'
import { parse } from 'graphql'

const isUniqueDirectiveKey = 'unique'
const defaultValueDirectiveKey = 'default'
const relationDirectiveKey = 'relation'

/**
 * Parses a datamodel given as DSL
 * to an internal representation, convenient for
 * working with.
 */
export default abstract class DatamodelParser {
  /**
   * Shorthand to parse the datamodel, given an SDL string.
   * @param schemaString The datamodel as SDL string.
   * @returns A list of types found in the datamodel.
   */
  public static parseFromSchemaString(schemaString: string) {
    const schema = parse(schemaString)
    return DatamodelParser.parseFromSchema(schema)
  }

  /**
   * Parses the datamodel from a graphql-js schema.
   * @param schema The graphql-js schema, representing the datamodel.
   * @returns A list of types found in the datamodel.
   */
  public static parseFromSchema(schema: any): IGQLType[] {
    const objectTypes: IGQLType[] = []

    // Parse all object types.
    for (const type of schema.definitions) {
      if (type.kind === 'ObjectTypeDefinition') {
        // For each object type, parse each field.
        const fields: IGQLField[] = []
        for (const field of type.fields) {
          if (field.kind === 'FieldDefinition') {
            // Check for type, kind, name and directives.
            const name = field.name.value
            const kind = DatamodelParser.parseKind(field.type, null)
            const fieldType = DatamodelParser.parseType(field.type)

            const isUnique =
              field.directives.filter(
                x => x.name.value === isUniqueDirectiveKey,
              ).length > 0
            const defaultValueDirective = field.directives.filter(
              x => x.name.value === defaultValueDirectiveKey,
            )
            const defaultValue =
              defaultValueDirective.length > 0
                ? defaultValueDirective[0].arguments[0].value.value
                : null

            const relationDirective = field.directives.filter(
              x => x.name.value === relationDirectiveKey,
            )
            const relationName =
              relationDirective.length > 0
                ? relationDirective[0].arguments[0].value.value
                : null

            fields.push({
              name,
              type: fieldType,
              relationName,
              defaultValue,
              isUnique,
              isList: kind === 'ListType',
              isRequired: kind === 'NonNullType',
              relatedField: null,
            })
          }
        }

        const isEmbedded =
          type.directives &&
          type.directives.length > 0 &&
          type.directives.some(d => d.name.value === 'embedded')

        objectTypes.push({
          name: type.name.value,
          fields,
          isEnum: false,
          isEmbedded,
        })
      }
    }

    // Parse all enum types
    for (const type of schema.definitions) {
      if (type.kind === 'EnumTypeDefinition') {
        const values: IGQLField[] = []
        for (const value of type.values) {
          if (value.kind === 'EnumValueDefinition') {
            const name = value.name.value

            // All props except name are ignored for enum defs.
            values.push(new GQLScalarField(name, 'String', false))
          }
        }

        objectTypes.push({
          name: type.name.value,
          fields: values,
          isEnum: true,
          isEmbedded: false,
        })
      }
    }

    // Now, find all types that we know,
    // and assign a proper type object instead
    // of the string.
    for (const typeA of objectTypes) {
      for (const fieldA of typeA.fields) {
        for (const typeB of objectTypes) {
          // At this stage, every type is a string
          if ((fieldA.type as string) === typeB.name) {
            fieldA.type = typeB
          }
        }
      }
    }

    // Connect all relations that are named.
    for (const typeA of objectTypes) {
      for (const fieldA of typeA.fields) {
        if (typeof fieldA.type === 'string') {
          continue // Assume scalar
        }

        if (fieldA.relationName !== null && fieldA.relatedField === null) {
          for (const fieldB of fieldA.type.fields) {
            if (fieldB.relationName === fieldA.relationName) {
              if (fieldB.type !== typeA) {
                throw new Error('Relation type mismatch.')
              }
              fieldA.relatedField = fieldB
              fieldB.relatedField = fieldA
              break
            }
          }
        }
      }
    }

    // Connect  obvious relations which are lacking the relatioName directive.
    // We explicitely DO NOT ignore fields with a given relationName, in accordance
    // to the prisma implementation.
    for (const typeA of objectTypes) {
      searchThroughAFields: for (const fieldA of typeA.fields) {
        if (typeof fieldA.type === 'string') {
          continue // Assume scalar.
        }
        if (fieldA.relatedField !== null) {
          continue // Nothing to do, already connected
        }

        for (const fieldA2 of typeA.fields) {
          if (fieldA2 !== fieldA && fieldA2.type === fieldA.type) {
            // Skip, A has mode than one fields of this relation type.
            continue searchThroughAFields
          }
        }

        const relationPairs: Array<{ a: IGQLField; b: IGQLField }> = []

        // Look for the opposite field by type.
        for (const fieldB of fieldA.type.fields) {
          if (fieldB.type === typeA) {
            if (fieldB !== fieldA) {
              // Don't connect self-referencing fields
              relationPairs.push({ a: fieldA, b: fieldB })
            }
          }
        }

        // Create relation iff we have found a single pair
        if (relationPairs.length === 1) {
          const [{ a, b }] = relationPairs
          a.relatedField = b
          b.relatedField = a
        }
      }
    }

    // That's it.
    // We could check our model here, if we wanted to.
    // Possible checks:
    // * Check if we still use strings for identifying types for non-scalar types
    // * Check if all double-sided relations are connected correctly
    // * Check for duplicate type names
    // * Check for conflicting relations
    return objectTypes
  }

  /**
   * Traverses an AST branch and finds the next type.
   * This will skip modifiers like NonNullType or ListType.
   * @param type
   */
  private static parseType(type: any) {
    if (type.type) {
      return DatamodelParser.parseType(type.type)
    } else if (type.kind !== 'NamedType') {
      throw new Error()
    }
    return type.name.value
  }

  /**
   * Traverses an AST branch and returns the modifier
   * of the type: Either ListType or NonNullType.
   * @param type
   * @param acc
   */
  private static parseKind(type: any, acc: any) {
    if (!acc) {
      acc = type.kind
    }

    // If we find list, we always take list
    if (type.kind === 'ListType') {
      return type.kind
    }

    // Non-null has higher prio than nullable
    if (type.kind === 'NonNullType') {
      acc = type.kind
    }

    // When we reach the end, return whatever we have stored.
    if (type.type) {
      return DatamodelParser.parseKind(type.type, acc)
    } else {
      return acc
    }
  }
}
