import { Node } from './types'
import {
  parse,
  buildASTSchema,
  DocumentNode,
  ObjectTypeDefinitionNode,
  FieldDefinitionNode,
  EnumTypeDefinitionNode,
} from 'graphql'
import { difference, isString, isNumber, isDate, isBoolean } from 'lodash'

interface Mapping {
  [typeName: string]: { [key: string]: string }
}

interface FieldsMap {
  [name: string]: FieldDefinitionNode
}

interface Types {
  [typeName: string]: {
    definition: ObjectTypeDefinitionNode
    fields: FieldsMap
  }
}

interface Enums {
  [enumName: string]: string[]
}

export class Validator {
  typesString: string
  ast: DocumentNode
  types: Types
  enums: Enums
  mapping: Mapping
  validators: { [key: string]: (type: any) => boolean } = {
    ID: isString,
    String: isString,
    Int: value => {
      let x
      if (isNaN(value)) {
        return false
      }
      x = parseFloat(value)
      /* tslint:disable-next-line */
      return (x | 0) === x
    },
    Float: isNumber,
    DateTime: date => {
      return (
        typeof date === 'string' &&
        /\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/.test(
          date,
        )
      )
    },
    Boolean: isBoolean,
  }
  constructor(typesString: string) {
    this.typesString = typesString
    this.ast = parse(typesString)
    this.types = this.astToTypes(this.ast)
    this.enums = this.astToEnums(this.ast)
    this.validators = {
      ...this.validators,
      ...this.makeEnumValidators(this.enums),
    }
  }

  astToTypes(ast: DocumentNode): Types {
    return ast.definitions.reduce((acc, curr: ObjectTypeDefinitionNode) => {
      if (curr.kind !== 'ObjectTypeDefinition') {
        return acc
      }
      return {
        ...acc,
        [curr.name.value]: {
          definition: curr,
          fields: curr.fields.reduce((acc2, curr2) => {
            return {
              ...acc2,
              [curr2.name.value]: curr2,
            }
          }, {}),
        },
      }
    }, {})
  }

  astToEnums(ast: DocumentNode): Enums {
    return ast.definitions.reduce((acc, curr: EnumTypeDefinitionNode) => {
      if (curr.kind !== 'EnumTypeDefinition') {
        return acc
      }
      return {
        ...acc,
        [curr.name.value]: curr.values.reduce((acc2, curr2) => {
          return [...acc2, curr2.name.value]
        }, []),
      }
    }, {})
  }

  makeEnumValidators(enums: Enums): { [enumName: string]: () => boolean } {
    return Object.keys(enums).reduce((acc, enumName) => {
      return {
        ...acc,
        [enumName]: value => enums[enumName].includes(value),
      }
    }, {})
  }

  getFieldNames(
    typeName: string,
    requiredOnly: boolean = false,
    includeRelations: boolean = false,
  ) {
    //
    const { definition } = this.types[typeName]
    return definition.fields
      .filter(field => {
        const nonNull = field.type.kind === 'NonNullType'
        const isRelation = field.directives
          ? field.directives.find(d => d.name.value === 'relation')
          : false
        if (!includeRelations && isRelation) {
          return false
        }

        return !requiredOnly || nonNull
      })
      .map(f => f.name.value)
  }

  resolveFieldName(typeName: string, fieldName: string): string {
    if (
      this.mapping &&
      this.mapping[typeName] &&
      this.mapping[typeName][fieldName]
    ) {
      return this.mapping[typeName][fieldName]
    }

    return fieldName
  }

  validateNode(obj: any) {
    this.checkTypeName(obj)
    this.checkRequiredFields(obj)
    this.checkUnknownFields(obj)
    this.checkType(obj)
    return true
  }

  checkTypeName(obj: any) {
    if (!obj._typeName) {
      throw new Error(
        `Object ${JSON.stringify(obj)} needs a _typeName property`,
      )
    }
    if (!this.types[obj._typeName]) {
      throw new Error(`Type ${obj._typeName} does not exist`)
    }
  }

  checkRequiredFields(obj: any) {
    const typeName = obj._typeName
    const requiredFieldNames = this.getFieldNames(typeName, true)

    const missingFieldNames = difference(requiredFieldNames, Object.keys(obj))
    if (missingFieldNames.length > 0) {
      throw new Error(
        `Object ${JSON.stringify(
          obj,
        )} lacks the following properties: ${missingFieldNames.join(', ')}`,
      )
    }
  }

  checkUnknownFields(obj: any) {
    const typeName = obj._typeName
    const knownKeys = ['_typeName'].concat(
      this.getFieldNames(typeName, false, false),
    )
    const unknownKeys = difference(Object.keys(obj), knownKeys)
    if (unknownKeys.length > 0) {
      throw new Error(
        `Object ${JSON.stringify(
          obj,
        )} has the following unknown properties: ${unknownKeys.join(', ')}`,
      )
    }
  }

  checkType(obj: any) {
    const typeName = obj._typeName
    const { definition, fields } = this.types[typeName]
    const fieldNames = Object.keys(obj).filter(f => f !== '_typeName')
    fieldNames.forEach(fieldName => {
      const value = obj[fieldName]
      this.validateValue(value, fields[fieldName])
    })
  }

  validateValue(value: any, field: FieldDefinitionNode) {
    if (this.isList(field)) {
      if (!Array.isArray(value)) {
        throw new Error(`Error for value ${value}: It has to be a list.`)
      }
      value.forEach(v => this.validateScalarValue(v, field))
    } else {
      this.validateScalarValue(value, field)
    }
  }

  validateScalarValue(value: any, field: FieldDefinitionNode) {
    const type = this.getDeepType(field)
    const typeName = type.name.value
    const validator = this.validators[typeName]
    if (!validator) {
      throw new Error(
        `Error for value ${JSON.stringify(
          value,
        )}. Field type ${typeName} has no validator defined`,
      )
    }

    const valid = validator(value)
    if (!valid) {
      throw new Error(
        `Value ${JSON.stringify(value)} for field ${
          field.name.value
        } is not a valid ${typeName}`,
      )
    }
  }

  getDeepType(field: FieldDefinitionNode) {
    let pointer = field.type as any
    while (pointer.type) {
      pointer = pointer.type
    }

    return pointer
  }

  isList(field: FieldDefinitionNode) {
    let pointer = field.type as any
    while (pointer.type) {
      if (pointer.kind === 'ListType') {
        return true
      }
      pointer = pointer.type
    }

    return false
  }
}
