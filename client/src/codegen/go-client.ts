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

const whereArgs = 7

export class GoGenerator extends Generator {
  // Tracks which types we've already printed.
  // At the moment, it only tracks FooParamsExec types.
  printedTypes: { [key: string]: boolean } = {}

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

  goTypeName(fieldType: FieldLikeType): string {
    let typ: string
    if(fieldType.isEnum) {
      typ = goCase(fieldType.typeName)
    } else {
      typ = this.scalarMapping[fieldType.typeName] || fieldType.typeName
    }

    if(fieldType.isList) {
      typ = "[]" + typ
    } else if(!fieldType.isNonNull) {
      typ = "*" + typ
    }
    return typ
  }

  shouldOmitEmpty(fieldType: FieldLikeType): boolean {
    return !fieldType.isNonNull
  }

  goStructTag(field: GraphQLField<any, any>): string {
    let s = "`json:\"" + field.name
    if(this.shouldOmitEmpty(this.extractFieldLikeType(field))) {
      s += ",omitempty"
    }
    s += "\"`"
    return s
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
        exec *prisma.Exec
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

          let sTyp = ""
          const meth = goCase(field.name) + "ParamsExec"

          if(!this.printedTypes[meth] && field.args.length > 0) {
            this.printedTypes[meth] = true
            sTyp = `
              type ${meth} struct {
                ${args
                  .map(arg => `${goCase(arg.name)} ${this.goTypeName(this.extractFieldLikeType(arg as GraphQLField<any, any>))}`).join('\n')
                }
              }`
          }

          if(field.args.length !== 0 && field.args.length !== whereArgs) {
            throw new Error(`unexpected argument count ${field.args.length}`)
          }
          if(field.args.length === whereArgs && !isList) {
            throw new Error("looks like a getMany query but doesn't return an array")
          }

          if (field.args.length > 0) {
            return sTyp + `
              func (instance *${type.name}Exec) ${goCase(field.name)}(params *${goCase(field.name)}ParamsExec) *${goCase(typeName)}ExecArray {
                var wparams *prisma.WhereParams
                if params != nil {
                  wparams = &prisma.WhereParams{
                    Where: params.Where,
                    OrderBy: (*string)(params.OrderBy),
                    Skip: params.Skip,
                    After: params.After,
                    Before: params.Before,
                    First: params.First,
                    Last: params.Last,
                  }
                }

                ret := instance.exec.Client.GetMany(
                  instance.exec,
                  wparams,
                  [3]string{"${field.args[0].type}", "${field.args[1].type}", "${typeName}"},
                  "${field.name}",
                  []string{${typeFields.join(',')}})

                  return &${goCase(typeName)}ExecArray{ret}
              }`
          } else {
            return sTyp + `
              func (instance *${type.name}Exec) ${goCase(field.name)}() *${goCase(typeName)}Exec {
                ret := instance.exec.Client.GetOne(
                  instance.exec,
                  nil,
                  [2]string{"", "${typeName}"},
                  "${field.name}",
                  []string{${typeFields.join(',')}})

                return &${goCase(typeName)}Exec{ret}
              }`
          }
        }).join('\n')}

      // Exec docs
      func (instance ${type.name}Exec) Exec(ctx context.Context) (${type.name}, error) {
        var v ${type.name}
        err := instance.exec.Exec(ctx, &v)
        return v, err
      }

      // ${type.name}ExecArray docs
      type ${type.name}ExecArray struct {
        exec *prisma.Exec
      }

      // Exec docs
      func (instance ${type.name}ExecArray) Exec(ctx context.Context) ([]${type.name}, error) {
        var v []${type.name}
        err := instance.exec.ExecArray(ctx, &v)
        return v, err
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
              const fieldType = this.extractFieldLikeType(field as GraphQLField<any, any>)

              return `${goCase(field.name)} ${this.goTypeName(fieldType)} ${this.goStructTag(field as GraphQLField<any, any>)}`
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
        exec *prisma.Exec
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
            const fieldType = this.extractFieldLikeType(
              field as GraphQLField<any, any>,
            )

            const typ = this.goTypeName(fieldType)
            return `${goCase(field.name)} ${typ} ${this.goStructTag(field as GraphQLField<any, any>)}`
          }).join('\n')}
          }`
    },

    GraphQLScalarType: (type: GraphQLScalarType): string => ``,

    GraphQLIDType: (type: GraphQLScalarType): string => ``,

    GraphQLEnumType: (type: GraphQLEnumType): string => {
      const enumValues = type.getValues()
      const typ = goCase(type.name)
      return `
        // ${type.name} docs
        type ${typ} string
        const (
          ${enumValues
            .map(
              v => `${typ}${goCase(v.name)} ${typ} = "${v.name}"`,
            )
            .join('\n')}
          )`
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

  opUpdateMany(field) {
    return `
      func (client *Client) ${goCase(field.name)} (params *${goCase(field.name)}Params) *prisma.BatchPayloadExec {
        return client.Client.UpdateMany(
          prisma.UpdateParams{
            Data: params.Data,
            Where: params.Where,
          },
          [2]string{"${field.args[0].type}", "${field.args[1].type}"},
          "${field.name}")
      }`
  }

  opUpdate(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params *${goCase(field.name)}Params) *${goCase(typeName)}Exec {
        ret := client.Client.Update(
                 prisma.UpdateParams{
                   Data: params.Data,
                   Where: params.Where,
                 },
                 [3]string{"${field.args[0].type}", "${field.args[1].type}", "${typeName}"},
                 "${field.name}",
                 []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
  }

  opDeleteMany(field) {
    return `
      func (client *Client) ${goCase(field.name)} (params *${this.getDeepType(field.args[0].type)}) *prisma.BatchPayloadExec {
        return client.Client.DeleteMany(params, "${field.args[0].type}", "${field.name}")
      }`
  }

  opDelete(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params *${this.getDeepType(field.args[0].type)}) *${goCase(typeName)}Exec {
        ret := client.Client.Delete(
          params,
          [2]string{"${field.args[0].type}", "${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
  }

  opGetOne(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params *${this.getDeepType(field.args[0].type)}) *${goCase(typeName)}Exec {
        ret := client.Client.GetOne(
          nil,
          params,
          [2]string{"${field.args[0].type}", "${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
  }

  opGetMany(field) {
    const { typeFields, typeName, isList } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params *${goCase(field.name)}Params) *${goCase(typeName)}Exec${isList ? `Array` : ``} {
        var wparams *prisma.WhereParams
        if params != nil {
          wparams = &prisma.WhereParams{
            Where: params.Where,
            OrderBy: (*string)(params.OrderBy),
            Skip: params.Skip,
            After: params.After,
            Before: params.Before,
            First: params.First,
            Last: params.Last,
          }
        }

        ret := client.Client.GetMany(
          nil,
          wparams,
          [3]string{"${field.args[0].type}", "${field.args[1].type}", "${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec${isList ? `Array` : ``} {
          ret,
        }
      }`
  }

  opCreate(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params * ${this.getDeepType(field.args[0].type)}) *${goCase(typeName)}Exec {
        ret := client.Client.Create(
          params,
          [2]string{"${field.args[0].type}", "${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
  }

  opUpsert(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params *${goCase(field.name)}Params) *${goCase(typeName)}Exec {
        var uparams *prisma.UpsertParams
        if params != nil {
          uparams = &prisma.UpsertParams{
            Where:  params.Where,
            Create: params.Create,
            Update: params.Update,
          }
        }
        ret := client.Client.Upsert(
          uparams,
          [4]string{"${field.args[0].type}", "${field.args[1].type}", "${field.args[2].type}","${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
  }

  // FIXME(dh): rename this, split it up, etc
  opNode() {
    return `
      func (client *Client) Node(id *ID) *NodeExec {
        exec := client.Client.Node(id)
        return &NodeExec{exec}
      }`
  }

  printOperation(fields, operation: string, options: RenderOptions) {
    return Object.keys(fields)
      .map(key => {
        const field = fields[key]

        let sParams = `
          // ${goCase(field.name)}Params docs
          type ${goCase(field.name)}Params struct {
            ${field.args
              .map(arg => {
                const fieldType = this.extractFieldLikeType(arg)
                const typ = this.goTypeName(fieldType)
                return `${goCase(arg.name)} ${typ} ${this.goStructTag(arg)}`
              })
              .join('\n')}
          }`

        const { isList } = this.extractFieldLikeType(field)
        let sOperation = ""

        // FIXME(dh): This is brittle. A model may conceivably be named "Many",
        // in which case updateMany would be updating a single instance of Many.
        // The same issue applies to many other prefixes.
        if(operation === "mutation" && field.name.startsWith("updateMany")) {
          sOperation = this.opUpdateMany(field)
        } else if(operation === "mutation" && field.name.startsWith("update")) {
          sOperation = this.opUpdate(field)
        } else if(operation === "mutation" && field.name.startsWith("deleteMany")) {
          sOperation = this.opDeleteMany(field)
        } else if(operation === "mutation" && field.name.startsWith("delete")) {
          sOperation = this.opDelete(field)
        } else if(operation === "mutation" && field.name.startsWith("create")) {
          sOperation = this.opCreate(field)
        } else if(operation === "mutation" && field.name.startsWith("upsert")) {
          sOperation = this.opUpsert(field)
        } else if(operation === "query" && !isList && field.args.length === 1 && field.name !== "node") {
          sOperation = this.opGetOne(field)
        } else if(operation === "query" && isList && field.args.length === whereArgs) {
          // XXX this query should check for isList
          // 7 arguments in a getMany query
          sOperation = this.opGetMany(field)
        } else if(operation === "query" && !isList && field.args.length === whereArgs && field.name.endsWith("Connection")) {
          // XXX connections were and are completely broken in this client.
        } else if(operation === "query" && field.name === "node") {
          sOperation = this.opNode()
        } else {
          throw new Error(`Don't know how to handle operation ${operation} on field ${field.name}`)
        }

        return sParams + sOperation
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

    // Code in fixed shouldn't contain any dynamic content.
    // It could equally live in its own file
    // to which generated code gets appened.
    const fixed = `
    // Code generated by Prisma CLI (https://github.com/prisma/prisma). DO NOT EDIT.

package prisma

import (
	"context"

	"github.com/prisma/go-lib"

	"github.com/machinebox/graphql"
)

// ID docs
type ID struct{}

// Types

type Client struct {
	Client *prisma.Client
}

func New(endpoint string, opts ...graphql.ClientOption) *Client {
  return &Client{
    Client: prisma.New(endpoint, opts...),
  }
}
`

    // Dynamic contains the parts of the generated code that are dynamically generated.
    const dynamic = `

var defaultEndpoint = ${this.printEndpoint(options)}

// Queries
${this.printOperation(queryFields, 'query', options)}

// Mutations
${this.printOperation(mutationFields, 'mutation', options)}

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

    return fixed + dynamic
  }
}
