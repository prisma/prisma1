import { Generator } from './Generator'
import {
  // isNonNullType,
  // isListType,
  // isScalarType,
  // isObjectType,
  // isEnumType,
  GraphQLObjectType,
  // GraphQLSchema,
  GraphQLUnionType,
  GraphQLInterfaceType,
  GraphQLInputObjectType,
  // GraphQLInputField,
  // GraphQLField,
  // GraphQLInputType,
  // GraphQLOutputType,
  // GraphQLWrappingType,
  // GraphQLNamedType,
  GraphQLScalarType,
  GraphQLEnumType,
  // GraphQLFieldMap,
  GraphQLObjectType as GraphQLObjectTypeRef,
  // printSchema,
} from 'graphql'

// import pluralize from 'pluralize'
import * as upperCamelCase from 'uppercamelcase'

const upperCamelCasePatched = (s: string) => {
  const cased = upperCamelCase(s)
  return cased === 'Id' ? 'ID' : cased
}

export class GoGenerator extends Generator {
  scalarMapping = {
    Int: 'int32', // TODO: What is our scala int
    String: 'string',
    ID: 'string',
    Float: 'float32', // TODO: What is our scala float
    Boolean: 'bool',
    DateTime: 'string',
    Json: 'map[string]interface{}',
    Long: 'int64', // TODO: This is not correct I think
  }

  graphqlRenderers = {
    GraphQLUnionType: (type: GraphQLUnionType): string => ``,

    GraphQLObjectType: (
      type:
        | GraphQLObjectTypeRef
        | GraphQLInputObjectType
        | GraphQLInterfaceType,
    ): string => {
      const fieldMap = type.getFields()
      return `// ${type.name} docs
      type ${type.name} struct {
                ${Object.keys(fieldMap)
                  .map(key => {
                    const field = fieldMap[key]

                    // TODO: Ok this can't go to production - hacky - need to find proper field definition and field name with Null + List properties.
                    // TODO: Add Nullability and array to fieldType later.
                    const fieldType = field.type
                      .toString()
                      .replace('!', '')
                      .replace('[', '')
                      .replace(']', '')
                      .trim()

                    return `${upperCamelCasePatched(field.name)} ${this
                      .scalarMapping[fieldType] || fieldType}`
                  })
                  .join('\n')}
            }
        `
    },

    GraphQLInterfaceType: (
      type:
        | GraphQLObjectTypeRef
        | GraphQLInputObjectType
        | GraphQLInterfaceType,
    ): string => ``,

    GraphQLInputObjectType: (
      type:
        | GraphQLObjectTypeRef
        | GraphQLInputObjectType
        | GraphQLInterfaceType,
    ): string => ``,

    GraphQLScalarType: (type: GraphQLScalarType): string => ``,

    GraphQLIDType: (type: GraphQLScalarType): string => ``,

    GraphQLEnumType: (type: GraphQLEnumType): string => ``,
  }

  render() {
    // TODO: Remove scalars
    const typeNames = this.getTypeNames()
    const typeMap = this.schema.getTypeMap()
    return `
package prisma

import (
	"context"
	"log"

	"github.com/machinebox/graphql"
)


${typeNames
      .map(key => {
        let type = typeMap[key]
        try {
          // TODO: This assumption is not correct for all types
          type = type as GraphQLObjectType
          return this.graphqlRenderers[type.constructor.name]
            ? this.graphqlRenderers[type.constructor.name](type)
            : null
        } catch (e) {
          console.log(type.name, type.constructor.name)
          return
        }
      })
      .join('\n')}
        `
  }

  // TODO: Move this to some utils because this is same as TypescriptGenerator
  getTypeNames() {
    const ast = this.schema
    // Create types
    return Object.keys(ast.getTypeMap())
      .filter(typeName => !typeName.startsWith('__'))
      .filter(typeName => typeName !== (ast.getQueryType() as any).name)
      .filter(
        typeName =>
          ast.getMutationType()
            ? typeName !== (ast.getMutationType()! as any).name
            : true,
      )
      .filter(
        typeName =>
          ast.getSubscriptionType()
            ? typeName !== (ast.getSubscriptionType()! as any).name
            : true,
      )
      .sort(
        (a, b) =>
          (ast.getType(a) as any).constructor.name <
          (ast.getType(b) as any).constructor.name
            ? -1
            : 1,
      )
  }
}
