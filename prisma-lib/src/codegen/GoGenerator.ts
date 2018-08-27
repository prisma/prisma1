import { Generator } from './Generator'
import {
  //   isNonNullType,
  //   isListType,
  //   isScalarType,
  //   isObjectType,
  //   isEnumType,
  //   GraphQLObjectType,
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
  GraphQLField,
  GraphQLObjectType,
} from 'graphql'

// import pluralize from 'pluralize'
import * as upperCamelCase from 'uppercamelcase'

const goCase = (s: string) => {
  const cased = upperCamelCase(s)
  return cased.startsWith('Id') ? `ID${cased.slice(2)}` : cased
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

  // TODO: Ok this can't go to production - hacky - need to find proper field definition and field name with Null + List properties.
  // TODO: Add Nullability and array to fieldType later.
  rawTypeName(type) {
    return type
      .toString()
      .replace('!', '')
      .replace('[', '')
      .replace(']', '')
      .trim()
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
      return `
      // ${type.name}Exec docs
      type ${type.name}Exec struct {
          stack []string
      }

      ${Object.keys(fieldMap)
        .filter(key => {
          const field = fieldMap[key]
          const deepTypeName = this.getDeepType(field.type)
          const deepType = this.schema.getType(deepTypeName)
          const isScalar =
            deepType!.constructor.name === 'GraphQLScalarType' ? true : false
          return !isScalar
        })
        .map(key => {
          const field = fieldMap[key]
          const deepTypeName = this.getDeepType(field.type).toString()
          return ` // ${goCase(field.name)} docs
        func (instance ${type.name}Exec) ${goCase(field.name)}() ${goCase(
            deepTypeName,
          )}Exec {
            return ${goCase(deepTypeName)}Exec{}
          }`
        })
        .join('\n')}

      // Exec docs
      func (instance ${type.name}Exec) Exec() ${type.name} {
        return ${type.name}{}
      }
      
      // ${type.name} docs
      type ${type.name} struct {
          ${Object.keys(fieldMap)
            .map(key => {
              const field = fieldMap[key]

              const fieldType = this.rawTypeName(field.type)

              return `${goCase(field.name)} ${this.scalarMapping[fieldType] ||
                fieldType} \`json:"${field.name}"\``
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
    ): string => {
      const fieldMap = type.getFields()
      return `
      // ${goCase(type.name)}Exec docs
      type ${goCase(type.name)}Exec interface {}

      // ${goCase(type.name)} docs
      type ${goCase(type.name)} interface {
        ${Object.keys(fieldMap).map(key => {
          const field = fieldMap[key]
          const fieldType = this.rawTypeName(field.type)
          return `${goCase(field.name)}() ${this.scalarMapping[fieldType] ||
            fieldType}`
        })}
      }`
    },

    GraphQLInputObjectType: (
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
              const fieldType = this.rawTypeName(field.type)

              return `${goCase(field.name)} ${
                (this.scalarMapping[fieldType] || fieldType) === type.name
                  ? `*`
                  : ``
              }${this.scalarMapping[fieldType] || fieldType} \`json:"${
                field.name
              }"\``
            })
            .join('\n')}
            }
        `
    },

    GraphQLScalarType: (type: GraphQLScalarType): string => ``,

    GraphQLIDType: (type: GraphQLScalarType): string => ``,

    GraphQLEnumType: (type: GraphQLEnumType): string => {
      const enumValues = type.getValues()
      return `
            // ${type.name} docs
            type ${type.name} string
            const (
                ${enumValues
                  .map(
                    v =>
                      `
                      // ${goCase(v.name)}${type.name} docs
                      ${goCase(v.name)}${type.name} ${type.name} = "${v.name}"`,
                  )
                  .join('\n')}
            )
        `
    },
  }

  getDeepType(type) {
    if (type.ofType) {
      return this.getDeepType(type.ofType)
    }
    return type
  }

  graphqlTypeRenderersForQuery = {
    GraphQLScalarType: (type: GraphQLScalarType) => {
      return ``
    },
    GraphQLObjectType: (type: GraphQLObjectType) => {
      const typeFields = type.getFields()
      return `${Object.keys(typeFields)
        .map(key => {
          const field = typeFields[key]
          const deepTypeName = this.getDeepType(field.type)
          const deepType = this.schema.getType(deepTypeName)
          const isScalar =
            deepType!.constructor.name === 'GraphQLScalarType' ? true : false
          //   const isScalar = isScalarType(deepType) // TODO: Find out why this breaks with duplicate "graphql" error
          return isScalar ? `${field.name}` : ``
        })
        .join('\n')}`
    },
    GraphQLInterfaceType: (type: GraphQLInterfaceType) => {
      return ``
    },
    GraphQLUnionType: (type: GraphQLUnionType) => {
      return ``
    },
    GraphQLEnumType: (type: GraphQLEnumType) => {
      return ``
    },
    GraphQLInputObjectType: (type: GraphQLInputObjectType) => {
      const typeFields = type.getFields()
      return `${Object.keys(typeFields)
        .map(key => {
          const field = typeFields[key]
          return `${field.name}`
        })
        .join('\n')}`
    },
  }

  printQuery(
    queryField: GraphQLField<any, any>,
    operation: 'query' | 'mutation',
  ) {
    const typeName = this.getDeepType(queryField.type)
    const type = this.schema.getType(typeName)
    return `${operation} ${goCase(
      queryField.name,
    )} (${this.printVariablesDefinition(queryField)}) {
        ${queryField.name} (${this.printVariables(queryField)}) {
            ${
              this.graphqlTypeRenderersForQuery[type!.constructor.name]
                ? this.graphqlTypeRenderersForQuery[type!.constructor.name](
                    type,
                  )
                : null
            }
        }
    }`
  }

  printVariablesDefinition(queryField: GraphQLField<any, any>) {
    const queryArgs = queryField.args
    return queryArgs
      .map(arg => {
        const argType = this.rawTypeName(arg.type)
        return `$${arg.name}: ${argType}`
      })
      .join(', ')
  }

  printVariables(queryField: GraphQLField<any, any>) {
    const queryArgs = queryField.args
    return queryArgs
      .map(arg => {
        // TODO: Go to the nested params to fetch the correct arg
        return `${arg.name}: $${arg.name}`
      })
      .join(',\n')
  }

  printArgs(queryField: GraphQLField<any, any>) {
    const queryArgs = queryField.args
    return (
      queryArgs
        .map(arg => {
          // TODO: Go to the nested params to fetch the correct arg
          return `"${arg.name}": params.${goCase(arg.name)}`
        })
        .join(',\n') + ','
    )
  }

  render() {
    const typeNames = this.getTypeNames()
    const typeMap = this.schema.getTypeMap()

    const queryType = this.schema.getQueryType()
    const queryFields = queryType!.getFields()

    const mutationType = this.schema.getMutationType()
    const mutationFields = mutationType!.getFields()
    return `
package prisma

import (
	"context"
    "log"
    "reflect"

	"github.com/machinebox/graphql"
)

// DB Type to represent the client
type DB struct {
	Endpoint string // TODO: Remove the Endpoint from here and print it where needed.
}

// Queries

${Object.keys(queryFields)
      .map(key => {
        const queryField = queryFields[key]
        const queryArgs = queryField.args
        return `
          // ${goCase(queryField.name)}Params docs
          type ${goCase(queryField.name)}Params struct {
            ${queryArgs
              .map(arg => {
                const argType = this.rawTypeName(arg.type)
                return `${goCase(arg.name)} ${this.scalarMapping[argType] ||
                  argType} \`json:"${arg.name}"\``
              })
              .join('\n')}
          }
          
          // ${goCase(queryField.name)} docs
          func (db DB) ${goCase(queryField.name)} (params ${goCase(
          queryField.name,
        )}Params) ${goCase(this.rawTypeName(queryField.type))}Exec {
          data := db.Request(\`${this.printQuery(queryField, 'query')}\`,
          map[string]interface{}{
              ${this.printArgs(queryField)}
          },
      )
      return data["${queryField.name}"]
        }`
      })
      .join('\n')}

      // Mutations

      ${Object.keys(mutationFields)
        .map(key => {
          const mutationField = mutationFields[key]
          const mutationArgs = mutationField.args
          return `
              // ${goCase(mutationField.name)}Params docs
              type ${goCase(mutationField.name)}Params struct {
                ${mutationArgs
                  .map(arg => {
                    const argType = this.rawTypeName(arg.type)
                    return `${goCase(arg.name)} ${this.scalarMapping[argType] ||
                      argType} \`json:"${arg.name}"\``
                  })
                  .join('\n')}
                }
              
              // ${goCase(mutationField.name)} docs
              func (db DB) ${goCase(mutationField.name)} (params ${goCase(
            mutationField.name,
          )}Params) ${goCase(this.rawTypeName(mutationField.type))}Exec {
              data := db.Request(\`${this.printQuery(
                mutationField,
                'mutation',
              )}\`,
              map[string]interface{}{
                  ${this.printArgs(mutationField)}
              },
          )
          return data["${mutationField.name}"]
            }`
        })
        .join('\n')}

// Types

${typeNames
      .map(key => {
        let type = typeMap[key]
        return this.graphqlRenderers[type.constructor.name]
          ? this.graphqlRenderers[type.constructor.name](type)
          : `// TODO: No GraphQL Renderer for Type ${type.name} of type ${
              type.constructor.name
            }`
      })
      .join('\n')}

      func isZeroOfUnderlyingType(x interface{}) bool {
        return reflect.DeepEqual(x, reflect.Zero(reflect.TypeOf(x)).Interface())
      }

      // Request Send a GraphQL operation request
// TODO: arg variables can be made optional via variadic func approach
func (db DB) Request(query string, variables map[string]interface{}) map[string]interface{} {
	// TODO: Error handling (both network, GraphQL and application level (missing node etc))
	// TODO: Add auth support

	req := graphql.NewRequest(query)
	client := graphql.NewClient(db.Endpoint)

	for key, value := range variables {
		if !isZeroOfUnderlyingType(value) {
			req.Var(key, value)
		}
	}

	ctx := context.Background()

	// var respData ResponseStruct
	var respData map[string]interface{} // TODO: Type this properly with a struct
	if err := client.Run(ctx, req, &respData); err != nil {
		log.Fatal(err)
	}
	return respData
}
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
