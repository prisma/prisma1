import { Generator } from '../Generator'
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

import { getTypeNames } from '../../utils/getTypeNames'

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
    if (fieldType.isEnum) {
      typ = goCase(fieldType.typeName)
    } else {
      typ = this.scalarMapping[fieldType.typeName] || fieldType.typeName
    }

    if (fieldType.isList) {
      typ = '[]' + typ
    } else if (!fieldType.isNonNull) {
      typ = '*' + typ
    }
    return typ
  }

  shouldOmitEmpty(fieldType: FieldLikeType): boolean {
    return !fieldType.isNonNull
  }

  goStructTag(field: GraphQLField<any, any>): string {
    let s = '`json:"' + field.name
    if (this.shouldOmitEmpty(this.extractFieldLikeType(field))) {
      s += ',omitempty'
    }
    s += '"`'
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
    const isNonNull =
      field.type.toString().indexOf('!') > -1 &&
      field.type.toString().indexOf('!]') === -1
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
              this.getDeepType(field.type).constructor.name === 'GraphQLScalarType' ||
                this.getDeepType(field.type).constructor.name === 'GraphQLEnumType'
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
      if (type.name === 'BatchPayload') {
        return ''
      }

      if (type.name.startsWith('Aggregate')) {
        // We're merging all Aggregate types into a single type
        return ``
      }

      return `
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
            // XXX this code is responsible for things like
            // previousValues, pageInfo, aggregate, edges, and relations.
            // It should probably be specialised like the rest of our code generation.

            const field = fieldMap[key] as GraphQLField<any, any>
            const args = field.args
            const { typeFields, typeName, isList } = this.extractFieldLikeType(
              field as GraphQLField<any, any>,
            )

            let sTyp = ''
            const meth = goCase(field.name) + 'ParamsExec'

            // TODO(dh): This type (FooParamsExec) is redundant.
            // If we have a relation article.authors -> [User],
            // then we can reuse UsersParams.
            // The only reason we can't do it right now
            // is because we don't have the base type's plural name available
            // (and appending a single s doesn't work for names like Mouse)
            if (!this.printedTypes[meth] && field.args.length > 0) {
              this.printedTypes[meth] = true
              sTyp = `
                type ${meth} struct {
                  ${args
                    .map(
                      arg =>
                        `${goCase(arg.name)} ${this.goTypeName(
                          this.extractFieldLikeType(arg as GraphQLField<
                            any,
                            any
                          >),
                        )}`,
                    )
                    .join('\n')}
                }`
            }

            if (field.args.length !== 0 && field.args.length !== whereArgs) {
              throw new Error(`unexpected argument count ${field.args.length}`)
            }
            if (field.args.length === whereArgs && !isList) {
              throw new Error(
                "looks like a getMany query but doesn't return an array",
              )
            }

            if (field.args.length > 0) {
              return (
                sTyp +
                `
                func (instance *${type.name}Exec) ${goCase(
                  field.name,
                )}(params *${goCase(field.name)}ParamsExec) *${goCase(
                  typeName,
                )}ExecArray {
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
                    [3]string{"${field.args[0].type}", "${
                  field.args[1].type
                }", "${typeName}"},
                    "${field.name}",
                    []string{${typeFields.join(',')}})

                  return &${goCase(typeName)}ExecArray{ret}
                }`
              )
            } else {
              if (
                type.name.endsWith('Connection') &&
                field.name === 'aggregate'
              ) {
                return (
                  sTyp +
                  `
                  func (instance *${type.name}Exec) ${goCase(
                    field.name,
                  )}(ctx context.Context) (Aggregate, error) {
                    ret := instance.exec.Client.GetOne(
                      instance.exec,
                      nil,
                      [2]string{"", "${typeName}"},
                      "${field.name}",
                      []string{${typeFields.join(',')}})

                    var v Aggregate
                    _, err := ret.Exec(ctx, &v)
                    return v, err
                  }`
                )
              }
              return (
                sTyp +
                `
                func (instance *${type.name}Exec) ${goCase(
                  field.name,
                )}() *${goCase(typeName)}Exec {
                  ret := instance.exec.Client.GetOne(
                    instance.exec,
                    nil,
                    [2]string{"", "${typeName}"},
                    "${field.name}",
                    []string{${typeFields.join(',')}})

                  return &${goCase(typeName)}Exec{ret}
                }`
              )
            }
          })
          .join('\n')}

          func (instance ${type.name}Exec) Exec(ctx context.Context) (*${
        type.name
      }, error) {
            var v ${type.name}
            ok, err := instance.exec.Exec(ctx, &v)
            if err != nil {
              return nil, err
            }
            if !ok {
              return nil, ErrNoResult
            }
            return &v, nil
          }

          func (instance ${
            type.name
          }Exec) Exists(ctx context.Context) (bool, error) {
            return instance.exec.Exists(ctx)
          }

          type ${type.name}ExecArray struct {
            exec *prisma.Exec
          }

          func (instance ${type.name}ExecArray) Exec(ctx context.Context) ([]${
        type.name
      }, error) {
            var v []${type.name}
            err := instance.exec.ExecArray(ctx, &v)
            return v, err
          }

        type ${type.name} struct {
          ${Object.keys(fieldMap)
            .filter(key => {
              const field = fieldMap[key]
              const { isScalar, isEnum } = this.extractFieldLikeType(
                field as GraphQLField<any, any>,
              )
              return isScalar || isEnum
            })
            .map(key => {
              const field = fieldMap[key]
              const fieldType = this.extractFieldLikeType(field as GraphQLField<
                any,
                any
              >)

              return `${goCase(field.name)} ${this.goTypeName(
                fieldType,
              )} ${this.goStructTag(field as GraphQLField<any, any>)}`
            })
            .join('\n')}
        }`
    },

    GraphQLInterfaceType: (
      type:
        | GraphQLObjectTypeRef
        | GraphQLInputObjectType
        | GraphQLInterfaceType,
    ): string => {
      if (type.name === 'Node') {
        // Don't emit code relating to generic node fetching
        return ''
      }
      const fieldMap = type.getFields()
      return `
      type ${goCase(type.name)}Exec struct {
        exec *prisma.Exec
      }

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
      return `
      type ${type.name} struct {
        ${Object.keys(fieldMap)
          .map(key => {
            const field = fieldMap[key]
            const fieldType = this.extractFieldLikeType(field as GraphQLField<
              any,
              any
            >)

            const typ = this.goTypeName(fieldType)
            return `${goCase(field.name)} ${typ} ${this.goStructTag(
              field as GraphQLField<any, any>,
            )}`
          })
          .join('\n')}
          }`
    },

    GraphQLScalarType: (type: GraphQLScalarType): string => ``,

    GraphQLIDType: (type: GraphQLScalarType): string => ``,

    GraphQLEnumType: (type: GraphQLEnumType): string => {
      const enumValues = type.getValues()
      const typ = goCase(type.name)
      return `
        type ${typ} string
        const (
          ${enumValues
            .map(v => `${typ}${goCase(v.name)} ${typ} = "${v.name}"`)
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
    const param = this.paramsType(field, 'updateMany')
    return (
      param.code +
      `
      func (client *Client) ${goCase(field.name)} (params ${
        param.type
      }) *BatchPayloadExec {
        exec := client.Client.UpdateMany(
          prisma.UpdateParams{
            Data: params.Data,
            Where: params.Where,
          },
          [2]string{"${field.args[0].type}", "${field.args[1].type}"},
          "${field.name}")
        return &BatchPayloadExec{exec}
      }`
    )
  }

  opUpdate(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    const param = this.paramsType(field, 'update')
    return (
      param.code +
      `
      func (client *Client) ${goCase(field.name)} (params ${
        param.type
      }) *${goCase(typeName)}Exec {
        ret := client.Client.Update(
                 prisma.UpdateParams{
                   Data: params.Data,
                   Where: params.Where,
                 },
                 [3]string{"${field.args[0].type}", "${
        field.args[1].type
      }", "${typeName}"},
                 "${field.name}",
                 []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
    )
  }

  opDeleteMany(field) {
    return `
      func (client *Client) ${goCase(field.name)} (params *${this.getDeepType(
      field.args[0].type,
    )}) *BatchPayloadExec {
        exec := client.Client.DeleteMany(params, "${field.args[0].type}", "${
      field.name
    }")
        return &BatchPayloadExec{exec}
      }`
  }

  opDelete(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params ${this.getDeepType(
      field.args[0].type,
    )}) *${goCase(typeName)}Exec {
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
      func (client *Client) ${goCase(field.name)} (params ${this.getDeepType(
      field.args[0].type,
    )}) *${goCase(typeName)}Exec {
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
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    const param = this.paramsType(field)
    return (
      param.code +
      `
      func (client *Client) ${goCase(field.name)} (params *${
        param.type
      }) *${goCase(typeName)}ExecArray {
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
          [3]string{"${field.args[0].type}", "${
        field.args[1].type
      }", "${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}ExecArray{ret}
      }`
    )
  }

  opGetConnection(field) {
    // TODO(dh): Connections are not yet implemented
    const { typeName } = this.extractFieldLikeType(field)
    const param = this.paramsType(field)
    return (
      param.code +
      `
      func (client *Client) ${goCase(field.name)} (params *${
        param.type
      }) (${goCase(typeName)}Exec) {
        panic("not implemented")
      }`
    )
  }

  opCreate(field) {
    const { typeFields, typeName } = this.extractFieldLikeType(field)
    return `
      func (client *Client) ${goCase(field.name)} (params ${this.getDeepType(
      field.args[0].type,
    )}) *${goCase(typeName)}Exec {
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
    const param = this.paramsType(field, 'upsert')
    return (
      param.code +
      `
      func (client *Client) ${goCase(field.name)} (params ${
        param.type
      }) *${goCase(typeName)}Exec {
        uparams := &prisma.UpsertParams{
          Where:  params.Where,
          Create: params.Create,
          Update: params.Update,
        }
        ret := client.Client.Upsert(
          uparams,
          [4]string{"${field.args[0].type}", "${field.args[1].type}", "${
        field.args[2].type
      }","${typeName}"},
          "${field.name}",
          []string{${typeFields.join(',')}})

        return &${goCase(typeName)}Exec{ret}
      }`
    )
  }

  paramsType(field, verb?: string) {
    let type = goCase(field.name) + 'Params'
    if (verb) {
      // Mangle the name from <verb><noun>Params to <noun><verb>Params.
      // When the noun is in its plural form, turn it into its singular form.

      let arg = field.args.find(arg => {
        return arg.name === 'where'
      })
      if (!arg) {
        throw new Error("couldn't find expected 'where' argument")
      }
      let match = arg.type.toString().match('^(.+)Where(?:Unique)?Input!?$')
      if (match === null) {
        throw new Error("couldn't determine type name")
      }
      type = match[1] + goCase(verb) + 'Params'
    }
    let code = `
      type ${type} struct {
        ${field.args
          .map(arg => {
            const fieldType = this.extractFieldLikeType(arg)
            const typ = this.goTypeName(fieldType)
            return `${goCase(arg.name)} ${typ} ${this.goStructTag(arg)}`
          })
          .join('\n')}
      }`
    return { code: code, type: type }
  }

  printOperation(fields, operation: string, options: RenderOptions) {
    return Object.keys(fields)
      .map(key => {
        const field = fields[key]

        const { isList } = this.extractFieldLikeType(field)

        // FIXME(dh): This is brittle. A model may conceivably be named "Many",
        // in which case updateMany would be updating a single instance of Many.
        // The same issue applies to many other prefixes.
        if (operation === 'mutation') {
          if (field.name.startsWith('updateMany')) {
            return this.opUpdateMany(field)
          }
          if (field.name.startsWith('update')) {
            return this.opUpdate(field)
          }
          if (field.name.startsWith('deleteMany')) {
            return this.opDeleteMany(field)
          }
          if (field.name.startsWith('delete')) {
            return this.opDelete(field)
          }
          if (field.name.startsWith('create')) {
            return this.opCreate(field)
          }
          if (field.name.startsWith('upsert')) {
            return this.opUpsert(field)
          }
          throw new Error(
            'unsupported mutation operation on field ' + field.name,
          )
        }

        if (operation === 'query') {
          if (!isList && field.args.length === 1 && field.name !== 'node') {
            return this.opGetOne(field)
          }
          if (isList && field.args.length === whereArgs) {
            return this.opGetMany(field)
          }
          if (
            !isList &&
            field.args.length === whereArgs &&
            field.name.endsWith('Connection')
          ) {
            return this.opGetConnection(field)
          }
          if (field.name === 'node') {
            // Don't emit generic Node fetching
            return ``
          }
          throw new Error('unsupported query operation on field ' + field.name)
        }

        throw new Error('unsupported operation ' + operation)
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

  printSecret(options: RenderOptions): string | null {
    if (!options.secret) {
      return `""`
    } else {
      if (options.secret!.startsWith('${process.env')) {
        // Find a better way to generate Go env construct
        const envVariable = `${options
          .secret!.replace('${process.env[', '')
          .replace(']}', '')}`
          .replace("'", '')
          .replace("'", '')
        return `os.Getenv("${envVariable}")`
      } else {
        return `\"${options.secret.replace("'", '').replace("'", '')}\"`
      }
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
  "errors"

	"github.com/prisma/prisma-client-lib-go"

	"github.com/machinebox/graphql"
)

var ErrNoResult = errors.New("query returned no result")

func Str(v string) *string { return &v }
func Int32(v int32) *int32 { return &v }
func Bool(v bool) *bool    { return &v }

type BatchPayloadExec struct {
	exec *prisma.BatchPayloadExec
}

func (exec *BatchPayloadExec) Exec(ctx context.Context) (BatchPayload, error) {
	bp, err := exec.exec.Exec(ctx)
    return BatchPayload(bp), err
}

type BatchPayload struct {
	Count int64 \`json:"count"\`
}

type Aggregate struct {
	Count int64 \`json:"count"\`
}

type Client struct {
	Client *prisma.Client
}

type Options struct {
  Endpoint  string
  Secret    string
}

func New(options *Options, opts ...graphql.ClientOption) *Client {
  endpoint := DefaultEndpoint
  secret   := Secret
	if options != nil {
    endpoint = options.Endpoint
    secret = options.Secret
	}
	return &Client{
		Client: prisma.New(endpoint, secret, opts...),
	}
}

func (client *Client) GraphQL(ctx context.Context, query string, variables map[string]interface{}) (map[string]interface{}, error) {
	return client.Client.GraphQL(ctx, query, variables)
}
`

    // Dynamic contains the parts of the generated code that are dynamically generated.
    const dynamic = `

var DefaultEndpoint = ${this.printEndpoint(options)}
var Secret          = ${this.printSecret(options)}

${this.printOperation(queryFields, 'query', options)}

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

  static replaceEnv(str: string): string {
    const regex = /\${env:(.*?)}/
    const match = regex.exec(str)
    // tslint:disable-next-line:prefer-conditional-expression
    if (match) {
      let before = trimQuotes(str.slice(0, match.index))
      before = before.length > 0 ? `"${before}" + ` : ''
      let after = trimQuotes(str.slice(match[0].length + match.index))
      after = after.length > 0 ? ` + "${after}"` : ''
      return GoGenerator.replaceEnv(
        `${before}os.Getenv("${match[1]}")${after}`.replace(/`/g, ''),
      )
    } else {
      return `\`${str}\``
    }
  }
}

function trimQuotes(str) {
  let copy = str
  if (copy[0] === '"') {
    copy = copy.slice(1)
  }

  if (copy.slice(-1)[0] === '"') {
    copy = copy.slice(0, -1)
  }

  return copy
}
