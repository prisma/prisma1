import { IGQLType, IGQLField, GQLScalarField, ISDL } from '../model'
import { parse } from 'graphql'
import { DirectiveKeys } from '../directives';

// TODO(ejoebstl): It would be good to have this Parser fill the directive field for types and models as well.

/**
 * Parses a datamodel given as DSL
 * to an internal representation, convenient for
 * working with.
 */
export default abstract class Parser {
  /**
   * Shorthand to parse the datamodel, given an SDL string.
   * @param schemaString The datamodel as SDL string.
   * @returns A list of types found in the datamodel.
   */
  public parseFromSchemaString(schemaString: string) {
    const schema = parse(schemaString)
    return this.parseFromSchema(schema)
  }

  /**
   * Parses the datamodel from a graphql-js schema.
   * @param schema The graphql-js schema, representing the datamodel.
   * @returns A list of types found in the datamodel.
   */
  public parseFromSchema(schema: any): ISDL {
    const types = [
      ...this.parseObjectTypes(schema),
      ...this.parseEnumTypes(schema),
    ]

    this.resolveRelations(types)

    // Sort types alphabetically
    types.sort(({ name: a }, { name: b }) => (a > b ? 1 : -1))

    // That's it.
    // We could check our model here, if we wanted to.
    // Possible checks:
    // * Check if we still use strings for identifying types for non-scalar types
    // * Check if all double-sided relations are connected correctly
    // * Check for duplicate type names
    // * Check for conflicting relations
    return {
      types
    }
  }

  /**
   * Checks if the given field is an ID field
   * @param field
   */
  protected abstract isIdField(field: any): boolean

  /**
   * Checks if the given field is read-only.
   * If the field is an ID field, this method is not called and
   * read-only is assumed.
   * @param field
   */
  protected abstract isReadOnly(field: any): boolean

  /**
   * Finds a directive on a field or type by name.
   * @param fieldOrType
   * @param name
   */
  protected getDirectiveByName(fieldOrType: any, name: string): any {
    const directive = fieldOrType.directives.filter(x => x.name.value === name)

    return directive.length > 0 ? directive[0] : null
  }

  /**
   * Checks if a directive on a given field or type ecists
   * @param fieldOrType
   * @param name
   */
  protected hasDirective(fieldOrType: any, name: string): boolean {
    return this.getDirectiveByName(fieldOrType, name) != null
  }

  /**
   * Checks if the given field is unique.
   * @param field
   */
  protected isUniqe(field: any): boolean {
    return this.hasDirective(field, DirectiveKeys.isUnique)
  }

  /**
   * Gets a fields default value. If no default
   * value is given, returns null.
   * @param field
   */
  protected getDefaultValue(field: any): any {
    const directive = this.getDirectiveByName(field, DirectiveKeys.default)
    const args = directive === null ? [] : directive.arguments.filter(x => x.name.value === 'value')
    return args.length !== 0 ? args[0].value.value : null
  }

  /**
   * Gets a fields relation name. If no relation
   * exists, returns null.
   * @param field
   */
  protected getRelationName(field: any): string | null {
    const directive = this.getDirectiveByName(field, DirectiveKeys.relation)
    if (directive && directive.arguments) {
      const nameArgument = directive.arguments.find(
        a => a.name.value === 'name',
      )
      return nameArgument ? nameArgument.value.value : null
    }

    return null
  }

  /**
   * Parses a model field, respects all
   * known directives.
   * @param field
   */
  protected parseField(field: any): IGQLField {
    const name = field.name.value

    const kind = this.parseKind(field.type, null)
    const fieldType = this.parseType(field.type)
    const isId = this.isIdField(field)
    const isUnique = isId || this.isUniqe(field)
    const isReadOnly = isId || this.isReadOnly(field)
    const defaultValue = this.getDefaultValue(field)
    const relationName = this.getRelationName(field)

    return {
      name,
      type: fieldType,
      relationName,
      defaultValue,
      isUnique,
      isList: kind === 'ListType',
      isRequired: kind === 'NonNullType',
      relatedField: null,
      isId,
      isReadOnly,
    }
  }

  /**
   * Checks if the given type is an embedded type.
   * @param type
   */
  protected abstract isEmbedded(type: any): boolean
  //  public isEmbedded(type: any): boolean {
  //    return type.directives &&
  //      type.directives.length > 0 &&
  //      type.directives.some(d => d.name.value === 'embedded')
  //  }

  /**
   * Parases an object type.
   * @param type
   */
  protected parseObjectType(type: any): IGQLType {
    const fields: IGQLField[] = []
    for (const field of type.fields) {
      if (field.kind === 'FieldDefinition') {
        // Check for type, kind, name and directives.
        fields.push(this.parseField(field))
      }
    }

    const isEmbedded = this.isEmbedded(type)

    return {
      name: type.name.value,
      fields,
      isEnum: false,
      isEmbedded,
    }
  }

  /**
   * Parses all object types in the schema.
   * @param schema
   */
  protected parseObjectTypes(schema: any): IGQLType[] {
    const objectTypes: IGQLType[] = []

    for (const type of schema.definitions) {
      if (type.kind === 'ObjectTypeDefinition') {
        objectTypes.push(this.parseObjectType(type))
      }
    }

    return objectTypes
  }

  /**
   * Parses all enum types in the schema.
   * @param schema
   */
  protected parseEnumTypes(schema: any): IGQLType[] {
    const enumTypes: IGQLType[] = []
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

        enumTypes.push({
          name: type.name.value,
          fields: values,
          isEnum: true,
          isEmbedded: false,
        })
      }
    }

    return enumTypes
  }

  /**
   * Resolves and connects all realtion fields found
   * in the given type list.
   * @param types
   */
  protected resolveRelations(types: IGQLType[]) {
    // Find all types that we know,
    // and assign a proper type object instead
    // of the string.
    for (const typeA of types) {
      for (const fieldA of typeA.fields) {
        for (const typeB of types) {
          // At this stage, every type is a string
          if ((fieldA.type as string) === typeB.name) {
            fieldA.type = typeB
          }
        }
      }
    }

    // Connect all relations that are named.
    for (const typeA of types) {
      for (const fieldA of typeA.fields) {
        if (typeof fieldA.type === 'string') {
          continue // Assume scalar
        }

        if (fieldA.relationName !== null && fieldA.relatedField === null) {
          for (const fieldB of fieldA.type.fields) {
            if (fieldB.relationName === fieldA.relationName) {
              if (fieldB.type !== typeA) {
                throw new Error(
                  `Relation type mismatch for relation ${fieldA.relationName}`,
                )
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
    for (const typeA of types) {
      searchThroughAFields: for (const fieldA of typeA.fields) {
        if (typeof fieldA.type === 'string') {
          continue // Assume scalar.
        }
        if (fieldA.relatedField !== null) {
          continue // Nothing to do, already connected
        }

        for (const fieldA2 of typeA.fields) {
          if (fieldA2 !== fieldA && fieldA2.type === fieldA.type) {
            // Skip, A has more than one fields of this relation type.
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
  }

  /**
   * Traverses an AST branch and finds the next type.
   * This will skip modifiers like NonNullType or ListType.
   * @param type
   */
  protected parseType(type: any) {
    if (type.type) {
      return this.parseType(type.type)
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
  protected parseKind(type: any, acc: any) {
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
      return this.parseKind(type.type, acc)
    } else {
      return acc
    }
  }
}
