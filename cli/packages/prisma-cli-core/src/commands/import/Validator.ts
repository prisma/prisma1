import { Node, ImportData, RelationTuple, RelationNode } from './types'
import {
  parse,
  DocumentNode,
  ObjectTypeDefinitionNode,
  FieldDefinitionNode,
  EnumTypeDefinitionNode,
} from 'graphql'
import { difference, isString, isNumber, isDate, isBoolean } from 'lodash'

export interface Mapping {
  [typeName: string]: { [key: string]: string }
}

export interface FieldsMap {
  [name: string]: FieldDefinitionNode
}

export interface Types {
  [typeName: string]: {
    definition: ObjectTypeDefinitionNode
    fields: FieldsMap
    requiredNonRelationScalarFieldNames: string[]
    requiredNonRelationListFieldNames: string[]
    allNonRelationListFieldNames: string[]
    allNonRelationScalarFieldNames: string[]
  }
}

export interface Enums {
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
    Json: value => {
      return typeof value === 'object'
    },
  }
  modelTypes: { [key: string]: boolean }
  constructor(typesString: string) {
    this.typesString = typesString
    this.ast = parse(typesString)
    this.modelTypes = this.collectModelTypes(this.ast)
    this.types = this.astToTypes(this.ast)
    this.enums = this.astToEnums(this.ast)
    this.validators = {
      ...this.validators,
      ...this.makeEnumValidators(this.enums),
    }
  }

  validateImportData(data: ImportData): true {
    if (!data.values) {
      throw new Error('Import data is missing the "values" property')
    }
    if (!Array.isArray(data.values)) {
      throw new Error(`Key "values" must be an array`)
    }
    if (data.valueType === 'nodes') {
      for (let i = 0, len = data.values.length; i < len; i++) {
        const node = data.values[i]
        this.validateNode(node)
      }
    }
    if (data.valueType === 'relations') {
      for (let i = 0, len = data.values.length; i < len; i++) {
        const node = data.values[i]
        this.validateRelationTuple(node as any)
      }
    }
    if (data.valueType === 'lists') {
      for (let i = 0, len = data.values.length; i < len; i++) {
        const node = data.values[i]
        this.validateListNode(node)
      }
    }

    return true
  }

  validateNode(obj: any): true {
    this.checkTypeName(obj)
    this.checkIdField(obj)
    this.checkRequiredFields(obj, false)
    this.checkUnknownFields(obj, false)
    this.checkType(obj, false)
    return true
  }

  validateListNode(obj: any): true {
    this.checkTypeName(obj)
    this.checkIdField(obj)
    this.checkUnknownFields(obj, true)
    this.checkType(obj, true)
    return true
  }

  validateRelationNode(node: RelationNode): true {
    this.checkTypeName(node)
    this.checkIdField(node)
    return true
  }

  validateRelationTuple(tuple: RelationTuple): true {
    if (!Array.isArray(tuple)) {
      throw new Error('Relation tuple must be an array')
    }

    if ((tuple as any).length !== 2) {
      throw new Error(`Relation tuple must have 2 nodes`)
    }

    for (let i = 0; i < 2; i++) {
      this.validateRelationNode(tuple[i])
    }

    const hasFieldName = tuple.reduce((acc, node) => {
      return this.checkFieldName(node) || acc
    }, false)

    if (!hasFieldName) {
      throw new Error(
        `Relation tuple ${JSON.stringify(
          tuple,
        )} must include a "fieldName" property`,
      )
    }

    return true
  }

  private checkFieldName(node: RelationNode): boolean {
    if (!node.fieldName) {
      return false
    }
    if (!this.types[node._typeName].fields[node.fieldName]) {
      throw new Error(
        `The "fieldName" property of node ${JSON.stringify(
          node,
        )} points to a non-existing fieldName "${node.fieldName}"`,
      )
    }
    return true
  }

  private collectModelTypes(ast: DocumentNode): { [key: string]: boolean } {
    return ast.definitions.reduce(
      (acc, curr: ObjectTypeDefinitionNode) => {
        if (curr.kind !== 'ObjectTypeDefinition') {
          return acc
        }
        return {
          ...acc,
          [curr.name.value]: true,
        }
      },
      {} as any,
    )
  }

  private astToTypes(ast: DocumentNode): Types {
    return ast.definitions.reduce(
      (acc, curr: ObjectTypeDefinitionNode) => {
        if (curr.kind !== 'ObjectTypeDefinition') {
          return acc
        }
        return {
          ...acc,
          [curr.name.value]: {
            definition: curr,
            fields: curr.fields!.reduce((acc2, curr2) => {
              return {
                ...acc2,
                [curr2.name.value]: curr2,
              }
            }, {}),
            allNonRelationScalarFieldNames: this.getFieldNames(
              curr,
              false,
              false,
            ),
            allNonRelationListFieldNames: this.getFieldNames(curr, false, true),
            requiredNonRelationScalarFieldNames: this.getFieldNames(curr, true),
            requiredNonRelationListFieldNames: this.getFieldNames(
              curr,
              true,
              true,
            ),
          },
        }
      },
      {} as Types,
    )
  }

  private astToEnums(ast: DocumentNode): Enums {
    return ast.definitions.reduce((acc, curr: EnumTypeDefinitionNode) => {
      if (curr.kind !== 'EnumTypeDefinition') {
        return acc
      }
      return {
        ...acc,
        [curr.name.value]: curr.values!.reduce((acc2, curr2) => {
          return [...acc2, curr2.name.value]
        }, []),
      }
    }, {})
  }

  private makeEnumValidators(
    enums: Enums,
  ): { [enumName: string]: () => boolean } {
    return Object.keys(enums).reduce((acc, enumName) => {
      return {
        ...acc,
        [enumName]: value => enums[enumName].includes(value),
      }
    }, {})
  }

  private getFieldNames(
    definition: ObjectTypeDefinitionNode,
    requiredOnly: boolean = false,
    listsOnly: boolean = false,
  ) {
    return definition
      .fields!.filter(field => {
        const nonNull = field.type.kind === 'NonNullType'
        const isRelation = field.directives
          ? field.directives.find(d => d.name.value === 'relation')
          : false
        if (isRelation) {
          return false
        }

        const typeName = this.getDeepType(field).name.value
        // if there is no validator, it's a relation or enum
        if (this.modelTypes[typeName]) {
          return false
        }

        let listBoolean = true
        const isList = this.isList(field)
        if ((listsOnly && !isList) || (!listsOnly && isList)) {
          listBoolean = false
        }

        return listBoolean && (!requiredOnly || nonNull)
      })
      .map(f => f.name.value)
  }

  private resolveFieldName(typeName: string, fieldName: string): string {
    if (
      this.mapping &&
      this.mapping[typeName] &&
      this.mapping[typeName][fieldName]
    ) {
      return this.mapping[typeName][fieldName]
    }

    return fieldName
  }

  private checkTypeName(obj: any) {
    if (!obj._typeName) {
      throw new Error(
        `Object ${JSON.stringify(obj)} needs a _typeName property`,
      )
    }
    if (!this.types[obj._typeName]) {
      throw new Error(`Type ${obj._typeName} does not exist`)
    }
  }

  private checkIdField(obj: any) {
    if (!obj.id) {
      throw new Error(`Object ${JSON.stringify(obj)} needs an id property`)
    }

    if (typeof obj.id !== 'string') {
      throw new Error(
        `The "id" of object ${JSON.stringify(obj)} needs to be a string`,
      )
    }
  }

  private checkRequiredFields(obj: any, listsOnly: boolean) {
    const typeName = obj._typeName
    const {
      requiredNonRelationListFieldNames,
      requiredNonRelationScalarFieldNames,
    } = this.types[typeName]

    const fieldNames = listsOnly
      ? requiredNonRelationListFieldNames
      : requiredNonRelationScalarFieldNames

    const missingFieldNames = difference(fieldNames, Object.keys(obj))
    if (missingFieldNames.length > 0) {
      throw new Error(
        `Object ${JSON.stringify(
          obj,
        )} lacks the following properties: ${missingFieldNames.join(', ')}`,
      )
    }
  }

  private checkUnknownFields(obj: any, includeLists: boolean) {
    const typeName = obj._typeName
    const {
      allNonRelationScalarFieldNames,
      allNonRelationListFieldNames,
    } = this.types[typeName]
    const fieldNames = includeLists
      ? allNonRelationListFieldNames
      : allNonRelationScalarFieldNames
    const knownKeys = ['_typeName', 'id', 'createdAt', 'updatedAt'].concat(
      fieldNames,
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

  private checkType(obj: any, listsOnly: boolean) {
    const typeName = obj._typeName
    const { definition, fields } = this.types[typeName]
    const fieldNames = Object.keys(obj).filter(f => f !== '_typeName')
    fieldNames.forEach(fieldName => {
      const value = obj[fieldName]
      if (!['createdAt', 'updatedAt', 'id'].includes(fieldName)) {
        const field = fields[fieldName]
        if (!field) {
          throw new Error(`Could not find field ${fieldName}`)
        }
        this.validateValue(value, field, listsOnly)
      }
    })
  }

  private validateValue(
    value: any,
    field: FieldDefinitionNode,
    listsOnly: boolean,
  ) {
    const isList = this.isList(field)
    if (isList && !listsOnly) {
      throw new Error(
        `List value ${value} mustn't be provided in a "nodes" definition`,
      )
    }
    if (!isList && listsOnly && field.name.value !== 'id') {
      throw new Error(
        `Single scalar value ${JSON.stringify(
          value,
        )} mustn't be provided in a "relations" definition`,
      )
    }
    if (isList) {
      if (!Array.isArray(value)) {
        throw new Error(`Error for value ${value}: It has to be a list.`)
      }
      value.forEach(v => this.validateScalarValue(v, field))
    } else {
      this.validateScalarValue(value, field)
    }
  }

  private validateScalarValue(value: any, field: FieldDefinitionNode) {
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

  private getDeepType(field: FieldDefinitionNode) {
    let pointer = field.type as any
    while (pointer.type) {
      pointer = pointer.type
    }

    return pointer
  }

  private isList(field: FieldDefinitionNode) {
    if (!field) {
      return false
    }
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
