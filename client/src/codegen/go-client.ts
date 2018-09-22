import { Generator } from './Generator'
import {
  GraphQLUnionType,
  GraphQLInterfaceType,
  GraphQLInputObjectType,
  GraphQLScalarType,
  GraphQLEnumType,
  GraphQLObjectType as GraphQLObjectTypeRef,
  GraphQLObjectType,
  GraphQLField,
  GraphQLArgument,
  GraphQLFieldMap,
  GraphQLInputFieldMap,
} from 'graphql'

import * as upperCamelCase from 'uppercamelcase'

import { getTypeNames } from '../utils/getTypeNames'

const goCase = (s: string) => {
  const cased = upperCamelCase(s)
  return cased.startsWith('Id') ? `ID${cased.slice(2)}` : cased
}

// This structure represents info about type of field, arg
export type FieldLikeType = {
  name: string
  typeName: string
  type: GraphQLInputObjectType
  typeFields: string[]
  isScalar: boolean
  isEnum: boolean
  args: GraphQLArgument[]
  isList: boolean
  isNonNull: boolean
  isInput: boolean
}

export interface RenderOptions {
  endpoint: string
  secret?: string
}

export class GoGenerator extends Generator {
  scalarMapping = {
    Int: 'int32',
    String: 'string',
    ID: 'string',
    Float: 'float32',
    Boolean: 'bool',
    DateTime: 'string',
    Json: 'map[string]interface{}',
    Long: 'int64',
  }

  extractFieldLikeType(field: GraphQLField<any, any>): FieldLikeType {
    const deepTypeName = this.getDeepType(field.type)
    const deepType = this.schema.getType(deepTypeName)
    const isScalar = deepType!.constructor.name === 'GraphQLScalarType'
    const isEnum = deepType!.constructor.name === 'GraphQLEnumType'
    const isInput = deepType!.constructor.name === 'GraphQLInputObjectType'
    const isList =
      field.type.toString().indexOf('[') === 0 &&
      field.type.toString().indexOf(']') > -1
    const isNonNull = field.type.toString().indexOf('!') > -1 && field.type.toString().indexOf('!]') === -1
    let fieldMap: GraphQLFieldMap<any, any> | GraphQLInputFieldMap | null = null
    if (deepType!.constructor.name === 'GraphQLObjectType') {
      fieldMap = (deepType as GraphQLObjectType).getFields()
    }
    if (deepType!.constructor.name === 'GraphQLInputObjectType') {
      fieldMap = (deepType as GraphQLInputObjectType).getFields()
    }
    const fields = Boolean(fieldMap)
      ? Object.keys(fieldMap!)
          .filter(key => {
            const field = fieldMap![key]
            return (
              this.getDeepType(field.type).constructor.name ===
              'GraphQLScalarType'
            )
          })
          .map(key => `"${fieldMap![key].name}"`)
      : []
    return {
      name: field.name,
      typeName: deepTypeName.toString(),
      type: deepType! as GraphQLInputObjectType,
      typeFields: fields,
      args: field.args,
      isScalar,
      isEnum,
      isList,
      isNonNull,
      isInput,
    }
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
        client    *prisma.Client
        stack []prisma.Instruction
      }

      ${Object.keys(fieldMap)
        .filter(key => {
          const field = fieldMap[key]
          const { isScalar, isEnum } = this.extractFieldLikeType(
            field as GraphQLField<any, any>,
          )
          return !isScalar && !isEnum
        })
        .map(key => {
          const field = fieldMap[key] as GraphQLField<any, any>
          const args = field.args
          const { typeFields, typeName, isList } = this.extractFieldLikeType(
            field as GraphQLField<any, any>,
          )
          return `
          ${args.length > 0 ? `
          type ${goCase(field.name)}ParamsExec struct {
            ${args
              .map(arg => `${goCase(arg.name)} *${this.scalarMapping[arg.type.toString()] || arg.type }`).join('\n')
          }
        }
          ` : ``}

          // ${goCase(field.name)} docs - executable for types
        func (instance *${type.name}Exec) ${goCase(field.name)}(${args.length > 0 ? `params *${goCase(field.name)}ParamsExec` : ``}) *${goCase(typeName.toString())}Exec${isList ? `Array` : ``} {
              var args []prisma.GraphQLArg

              ${args.length > 0 ? `
              if params != nil {
                ${args.map(arg => `
                if params.${goCase(arg.name)} != nil {
                  args = append(args, prisma.GraphQLArg{
                    Name: "${arg.name}",
                    Key: "${arg.name}",
                    TypeName: "${this.scalarMapping[arg.type.toString()] || arg.type }",
                    Value: params.${goCase(arg.name)},
                  })
                }
                `).join('\n')}
              }
              ` : ``}

              instance.stack = append(instance.stack, prisma.Instruction{
                Name: "${field.name}",
                Field: prisma.GraphQLField{
                  Name: "${field.name}",
                  TypeName: "${typeName}",
                  TypeFields: ${`[]string{${typeFields
                    .map(f => f)
                    .join(',')}}`},
                },
                Operation: "",
                Args: args,
              })
            return &${goCase(typeName.toString())}Exec${isList ? `Array` : ``}{
              client: instance.client,
              stack: instance.stack,
            }
          }`
        })
        .join('\n')}

      // Exec docs
      func (instance ${type.name}Exec) Exec() (${type.name}, error) {
        var allArgs []prisma.GraphQLArg
        variables := make(map[string]interface{})
        for instructionKey := range instance.stack {
          instruction := &instance.stack[instructionKey]
          if instance.client.Debug {
            fmt.Println("Instruction Exec: ", instruction)
          }
          for argKey := range instruction.Args {
            arg := &instruction.Args[argKey]
            if instance.client.Debug {
              fmt.Println("Instruction Arg Exec: ", instruction)
            }
            isUnique := false
            for !isUnique {
              isUnique = true
              for key, existingArg := range allArgs {
                if existingArg.Name == arg.Name {
                  isUnique = false
                  arg.Name = arg.Name + "_" + strconv.Itoa(key)
                  if instance.client.Debug {
                    fmt.Println("Resolving Collision Arg Name: ", arg.Name)
                  }
                  break
                }
              }
            }
            if instance.client.Debug {
              fmt.Println("Arg Name: ", arg.Name)
            }
            allArgs = append(allArgs, *arg)
            variables[arg.Name] = arg.Value
          }
        }
        query := instance.client.ProcessInstructions(instance.stack)
        if instance.client.Debug {
          fmt.Println("Query Exec:", query)
          fmt.Println("Variables Exec:", variables)
        }
        data, err := instance.client.GraphQL(query, variables)
        if instance.client.Debug {
          fmt.Println("Data Exec:", data)
          fmt.Println("Error Exec:", err)
        }

        var genericData interface{} // This can handle both map[string]interface{} and []interface[]

        // Is unpacking needed
        dataType := reflect.TypeOf(data)
        if !prisma.IsArray(dataType) {
          unpackedData := data
          for _, instruction := range instance.stack {
            if instance.client.Debug {
              fmt.Println("Original Unpacked Data Step Exec:", unpackedData)
            }
            if prisma.IsArray(unpackedData[instruction.Name]) {
              genericData = (unpackedData[instruction.Name]).([]interface{})
              break
            } else {
              unpackedData = (unpackedData[instruction.Name]).(map[string]interface{})
            }
            if instance.client.Debug {
              fmt.Println("Partially Unpacked Data Step Exec:", unpackedData)
            }
            if instance.client.Debug {
              fmt.Println("Unpacked Data Step Instruction Exec:", instruction.Name)
              fmt.Println("Unpacked Data Step Exec:", unpackedData)
              fmt.Println("Unpacked Data Step Type Exec:", reflect.TypeOf(unpackedData))
            }
            genericData = unpackedData
          }
        }
        if instance.client.Debug {
          fmt.Println("Data Unpacked Exec:", genericData)
        }

        var decodedData ${type.name}
        mapstructure.Decode(genericData, &decodedData)
        if instance.client.Debug {
          fmt.Println("Data Exec Decoded:", decodedData)
        }
        return decodedData, err
      }

      // ${type.name}ExecArray docs
      type ${type.name}ExecArray struct {
        client    *prisma.Client
        stack []prisma.Instruction
      }

      // Exec docs
      func (instance ${type.name}ExecArray) Exec() ([]${type.name}, error) {
        query := instance.client.ProcessInstructions(instance.stack)
        variables := make(map[string]interface{})
        for _, instruction := range instance.stack {
          if instance.client.Debug {
            fmt.Println("Instruction Exec: ", instruction)
          }
          for _, arg := range instruction.Args {
            if instance.client.Debug {
              fmt.Println("Instruction Arg Exec: ", instruction)
            }
            variables[arg.Name] = arg.Value
          }
        }
        if instance.client.Debug {
          fmt.Println("Query Exec:", query)
          fmt.Println("Variables Exec:", variables)
        }
        data, err := instance.client.GraphQL(query, variables)
        if instance.client.Debug {
          fmt.Println("Data Exec:", data)
          fmt.Println("Error Exec:", err)
        }

        var genericData interface{} // This can handle both map[string]interface{} and []interface[]

        // Is unpacking needed
        dataType := reflect.TypeOf(data)
        if !prisma.IsArray(dataType) {
          unpackedData := data
          for _, instruction := range instance.stack {
            if instance.client.Debug {
              fmt.Println("Original Unpacked Data Step Exec:", unpackedData)
            }
            if prisma.IsArray(unpackedData[instruction.Name]) {
              genericData = (unpackedData[instruction.Name]).([]interface{})
              break
            } else {
              unpackedData = (unpackedData[instruction.Name]).(map[string]interface{})
            }
            if instance.client.Debug {
              fmt.Println("Partially Unpacked Data Step Exec:", unpackedData)
            }
            if instance.client.Debug {
              fmt.Println("Unpacked Data Step Instruction Exec:", instruction.Name)
              fmt.Println("Unpacked Data Step Exec:", unpackedData)
              fmt.Println("Unpacked Data Step Type Exec:", reflect.TypeOf(unpackedData))
            }
            genericData = unpackedData
          }
        }
        if instance.client.Debug {
          fmt.Println("Data Unpacked Exec:", genericData)
        }

        var decodedData []${type.name}
        mapstructure.Decode(genericData, &decodedData)
        if instance.client.Debug {
          fmt.Println("Data Exec Decoded:", decodedData)
        }
        return decodedData, err
      }

      // ${type.name} docs - generated with types
      type ${type.name} struct {
          ${Object.keys(fieldMap)
            .filter(key => {
              const field = fieldMap[key]
              const {
                isScalar,
              } = this.extractFieldLikeType(field as GraphQLField<any, any>)
              return isScalar
            })
            .map(key => {
              const field = fieldMap[key]
              const {
                typeName,
                isNonNull,
                isScalar,
              } = this.extractFieldLikeType(field as GraphQLField<any, any>)
              return `${goCase(field.name)} ${isScalar ? `` : `*`}${this
                .scalarMapping[typeName] || typeName} \`json:"${field.name}${
                isNonNull ? `` : `,omitempty`
              }"\``
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
      type ${goCase(type.name)}Exec struct {
        client    *prisma.Client
        stack []prisma.Instruction
      }

      // ${goCase(type.name)} docs - generated with types in GraphQLInterfaceType
      type ${goCase(type.name)} interface {
        ${Object.keys(fieldMap).map(key => {
          const field = fieldMap[key]
          const { typeName } = this.extractFieldLikeType(field as GraphQLField<
            any,
            any
          >)
          return `${goCase(field.name)}() ${this.scalarMapping[typeName] ||
            typeName}`
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
      return `// ${type.name} input struct docs
      type ${type.name} struct {
          ${Object.keys(fieldMap)
            .map(key => {
              const field = fieldMap[key]
              const { typeName } = this.extractFieldLikeType(
                field as GraphQLField<any, any>,
              )

              return `${goCase(field.name)} *${this.scalarMapping[typeName] ||
                typeName} \`json:"${field.name},omitempty"\``
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
          const { isScalar } = this.extractFieldLikeType(field)
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

  printOperation(fields, operation: string, options: RenderOptions) {
    return Object.keys(fields)
      .map(key => {
        const field = fields[key]
        const args = field.args
        const { typeFields, typeName, isList } = this.extractFieldLikeType(
          field,
        )

        const whereArgs = args.filter(arg => arg.name === 'where')
        let whereArg = null
        if (whereArgs.length > 0) {
          whereArg = whereArgs[0]
        }

        return `

          ${
            operation === 'query' && !isList && whereArg
              ? `
              // Exists

              // ${goCase(field.name)} exists docs
              func (exists *Exists) ${goCase(field.name)}(params *${goCase(
                  this.getDeepType((whereArg! as any).type).toString(),
                )}) bool {
                endpoint := exists.Endpoint
                if endpoint == "" {
                  endpoint = ${this.printEndpoint(options)}
                }
                client := &Client{
					Client: &prisma.Client{
						Endpoint: endpoint,
						Debug: exists.Debug,
                  },
                }
                data, err := client.${goCase(field.name)}(
                  ${
                    args.length === 1
                      ? `params,`
                      : `&${goCase(field.name)}Params{
                    Where: params,
                  },`
                  }
                ).Exec()
                if err != nil {
                  if client.Client.Debug {
                    fmt.Println("Error Exists", err)
                  }
                  return false
                }
                if prisma.IsZeroOfUnderlyingType(data) {
                  return false
                }
                return true
              }
          `
              : ``
          }

          // ${goCase(field.name)}Params docs
          type ${goCase(field.name)}Params struct {
            ${args
              .map(arg => {
                const { typeName, isNonNull } = this.extractFieldLikeType(arg)
                return `${goCase(arg.name)} *${this.scalarMapping[typeName] ||
                  typeName} \`json:"${arg.name}${
                  isNonNull ? `` : `,omitempty`
                }"\``
              })
              .join('\n')}
          }

          // ${goCase(field.name)} docs - generated while printing operation - ${operation}
          func (client *Client) ${goCase(field.name)} (${
          args.length === 1
            ? `params *${this.getDeepType(args[0].type)}`
            : `params *${goCase(field.name)}Params`
        }) *${goCase(typeName)}Exec${isList ? `Array` : ``} {

          stack := make([]prisma.Instruction, 0)
          var args []prisma.GraphQLArg
          ${args
            .map(arg => {
              return `if params != nil ${
                args.length === 1 ? `` : `&& params.${goCase(arg.name)} != nil`
              } {
                args = append(args, prisma.GraphQLArg{
                  Name: "${arg.name}",
                  Key: "${arg.name}",
                  TypeName: "${arg.type}",
                  Value: *params${
                    args.length === 1 ? `` : `.${goCase(arg.name)}`
                  },
                })
              }`
            })
            .join('\n')}

          stack = append(stack, prisma.Instruction{
            Name: "${field.name}",
            Field: prisma.GraphQLField{
              Name: "${field.name}",
              TypeName: "${typeName}",
              TypeFields: ${`[]string{${typeFields.map(f => f).join(',')}}`},
            },
            Operation: "${operation}",
            Args: args,
          })

          return &${goCase(typeName)}Exec${isList ? `Array` : ``}{
            client: client.Client,
            stack: stack,
          }
        }`
      })
      .join('\n')
  }

  printEndpoint(options: RenderOptions) {
    if (options.endpoint.startsWith('process.env')) {
      // Find a better way to generate Go env construct
      const envVariable = `${options.endpoint
        .replace('process.env[', '')
        .replace(']', '')}`
        .replace("'", '')
        .replace("'", '')
      return `os.Getenv("${envVariable}")`
    } else {
      return `\"${options.endpoint.replace("'", '').replace("'", '')}\"`
    }
  }

  render(options: RenderOptions) {
    const typeNames = getTypeNames(this.schema)
    const typeMap = this.schema.getTypeMap()

    const queryType = this.schema.getQueryType()
    const queryFields = queryType!.getFields()

    const mutationType = this.schema.getMutationType()
    const mutationFields = mutationType!.getFields()
    return `
// Code generated by Prisma CLI (https://github.com/prisma/prisma). DO NOT EDIT.

package prisma

import (
	"fmt"
	"reflect"
	"strconv"

	"github.com/prisma/go-lib"

	"github.com/mitchellh/mapstructure"
)

// ID docs
type ID struct{}

// Queries
${this.printOperation(queryFields, 'query', options)}

// Mutations
${this.printOperation(mutationFields, 'mutation', options)}

// Types

type Exists struct{
	Endpoint string
	Debug bool
}

type Client struct {
	Client *prisma.Client
}

${typeNames
      .map(key => {
        let type = typeMap[key]
        return this.graphqlRenderers[type.constructor.name]
          ? this.graphqlRenderers[type.constructor.name](type)
          : `// No GraphQL Renderer for Type ${type.name} of type ${
              type.constructor.name
            }`
      })
      .join('\n')}
        `
  }
}
