import { Generator } from './Generator'
import {
  GraphQLUnionType,
  GraphQLInterfaceType,
  GraphQLInputObjectType,
  GraphQLScalarType,
  GraphQLEnumType,
  GraphQLObjectType as GraphQLObjectTypeRef,
  GraphQLObjectType,
  isListType,
  GraphQLField,
  GraphQLArgument,
  GraphQLFieldMap,
  GraphQLInputFieldMap,
  // GraphQLFieldMap,
  // GraphQLInputFieldMap,
} from 'graphql'

import * as upperCamelCase from 'uppercamelcase'

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

  extractFieldLikeType(field: GraphQLField<any, any>): FieldLikeType {
    const deepTypeName = this.getDeepType(field.type)
    const deepType = this.schema.getType(deepTypeName)
    const isScalar = deepType!.constructor.name === 'GraphQLScalarType'
    const isEnum = deepType!.constructor.name === 'GraphQLEnumType'
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
      typeName: deepTypeName,
      type: deepType! as GraphQLInputObjectType,
      typeFields: fields,
      args: field.args,
      isScalar,
      isEnum,
    }
  }

  // TODO: Hacky - need to find proper field definition and field name with Null + List properties.
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
        db    DB
        stack []Instruction // TODO: This will be map[string]interface{} to support parallel stacks
      }
      ${Object.keys(fieldMap)
        .filter(key => {
          const field = fieldMap[key]
          const deepTypeName = this.getDeepType(field.type)
          const deepType = this.schema.getType(deepTypeName)
          const isScalar = deepType!.constructor.name === 'GraphQLScalarType'
          const isEnum = deepType!.constructor.name === 'GraphQLEnumType'
          // const { isScalar, isEnum } = this.extractFieldLikeType(
          //   field as GraphQLField<any, any>,
          // )
          return !isScalar && !isEnum
        })
        .map(key => {
          const field = fieldMap[key] as GraphQLField<any, any>
          const deepTypeName = this.getDeepType(field.type)
          // const deepType = this.schema.getType(deepTypeName)
          const args = field.args
          const { typeFields } = this.extractFieldLikeType(
            field as GraphQLField<any, any>,
          )
          return ` // ${goCase(field.name)} docs - executable for types
        func (instance *${type.name}Exec) ${goCase(field.name)}(${args
            .map(arg => `${arg.name} ${arg.type}`)
            .join(',')}) *${goCase(deepTypeName.toString())}Exec {
              var args []interface{}
              ${args.map(arg => `args = append(args, ${arg.name})`)}
              instance.stack = append(instance.stack, Instruction{
                name: "${field.name}",
                field: GraphQLField{
                  name: "${field.name}",
                  typeName: "${deepTypeName}", // TODO: We might need to full field object later to get array and non-null properties or add them as additional fields  // TODO: We might need to full field object later to get array and non-null properties or add them as additional fields
                  typeFields: ${`[]string{${typeFields
                    .map(f => f)
                    .join(',')}}`},
                },
                operation: "", // TODO: This is not a top level query, no operation
                args: args,
              })
            return &${goCase(deepTypeName.toString())}Exec{
              db: instance.db,
              stack: instance.stack,
            }
          }`
        })
        .join('\n')}

      // Exec docs
      func (instance ${type.name}Exec) Exec() ${type.name} {
        instance.db.ProcessInstructions(instance.stack)
        return ${type.name}{}
      }
      
      // ${type.name} docs
      type ${type.name} struct {
          ${Object.keys(fieldMap)
            .map(key => {
              const field = fieldMap[key]

              const fieldType = this.rawTypeName(field.type)

              return `${goCase(field.name)} ${this.scalarMapping[fieldType] ||
                fieldType} \`json:"${field.name},omitempty"\``
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
        db    DB
        stack []Instruction
      }

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
              },omitempty"\``
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
          const isScalar = deepType!.constructor.name === 'GraphQLScalarType'
          // const isEnum = deepType!.constructor.name === 'GraphQLEnumType'
          // const { isScalar } = this.extractFieldLikeType(field)
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

  printOperation(fields, operation: string) {
    return Object.keys(fields)
      .map(key => {
        const field = fields[key]
        const deepTypeName = this.getDeepType(field.type)
        // const deepType = this.schema.getType(deepTypeName)
        // const isScalar = deepType!.constructor.name === 'GraphQLScalarType'
        // const isEnum = deepType!.constructor.name === 'GraphQLEnumType'
        // const { typeName } = this.extractFieldLikeType(field)
        const args = field.args
        return `
          // ${goCase(field.name)}Params docs
          type ${goCase(field.name)}Params struct {
            ${args
              .map(arg => {
                const argType = this.rawTypeName(arg.type)
                return `${goCase(arg.name)} ${this.scalarMapping[argType] ||
                  argType} \`json:"${arg.name},omitempty"\``
              })
              .join('\n')}
          }
          
          // ${goCase(field.name)} docs
          func (db DB) ${goCase(field.name)} (params ${goCase(
          field.name,
        )}Params) *${isListType(field.type) ? `[]` : ``}${goCase(
          this.rawTypeName(field.type),
        )}Exec {

          stack := make([]Instruction, 0)
          var args []interface{}
          ${args
            .map(arg => {
              return `args = append(args, params.${goCase(arg.name)})`
            })
            .join('\n')}
          
          stack = append(stack, Instruction{
            name: "${field.name}",
            field: GraphQLField{
              name: "${field.name}",
              typeName: "${deepTypeName}", // TODO: We might need to full field object later to get array and non-null properties or add them as additional fields
            },
            operation: "${operation}",
            args: args,
          })

      return &${isListType(field.type) ? `[]` : ``}${goCase(
          this.rawTypeName(field.type),
        )}Exec{
          db: db,
          stack: stack,
        }
        }`
      })
      .join('\n')
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
  "fmt"
  "bytes"
  "encoding/json"
  "text/template"

	"github.com/machinebox/graphql"
)

// GraphQLField docs
type GraphQLField struct {
  name string
  typeName string
  typeFields []string
}

// Instruction docs
type Instruction struct {
  name string
  field GraphQLField
  operation string
	args []interface{}
}

// DB Type to represent the client
type DB struct {
  Endpoint string // TODO: Remove the Endpoint from here and print it where needed.
  Debug bool
}

// ProcessInstructions docs
func (db *DB) ProcessInstructions(stack []Instruction) string {
	query := make(map[string]interface{})
	args := make(map[string]interface{})
	for i := len(stack) - 1; i >= 0; i-- {
		instruction := stack[i]
		if db.Debug {
			fmt.Println("Instruction: ", instruction)
		}
		if len(query) == 0 {
			query[instruction.name] = instruction.field.typeFields
			args[instruction.name] = instruction.args
		} else {
			previousInstruction := stack[i+1]
			query[instruction.name] = map[string]interface{}{
				previousInstruction.name: query[previousInstruction.name],
			}
			args[instruction.name] = instruction.args
			delete(query, previousInstruction.name)
		}
	}

	if db.Debug {
		fmt.Println("Final Query:", query)
		fmt.Println("Final Args:", args)
	}

	// TODO: Make this recursive - current depth = 4
	queryTemplateString := \`
    query Q {
      {{ range $k, $v := $ }}
      {{ if isArray $v }}
        {{ range $k1, $v1 := $v }}
          {{ $v1 }}
        {{end}}
      {{ else }}
        {{ $k }} {
          {{ range $k, $v := $v }}
          {{ if isArray $v }}
            {{ $k }} { 
              {{ range $k1, $v1 := $v }}
                {{ $v1 }}
              {{end}}
            }
          {{ else }}
            {{ $k }} {
              {{ range $k, $v := $v }}
                {{ if isArray $v }}
                  {{ $k }} { 
                    {{ range $k1, $v1 := $v }}
                      {{ $v1 }}
                    {{end}}
                  }
                {{ else }}
                  {{ $k }} {
                    {{ range $k, $v := $v }}
                      {{ if isArray $v }}
                        {{ $k }} { 
                          {{ range $k1, $v1 := $v }}
                            {{ $v1 }}
                          {{end}}
                        }
                      {{ else }}
                        {{ $k }} {
                          id
                        }
                      {{ end }}
                      {{ end }}
                  }
                {{ end }}
                {{ end }}
            }
          {{ end }}
          {{ end }}
        }
      {{ end }}
      {{ end }}
    }
  \`

	templateFunctions := template.FuncMap{
		"isArray": func(i interface{}) bool {
			v := reflect.ValueOf(i)
			fmt.Println(v.Kind())
			switch v.Kind() {
			case reflect.Array:
				return true
			case reflect.Slice:
				return true
			default:
				return false
			}
		},
	}

	queryTemplate, err := template.New("query").Funcs(templateFunctions).Parse(queryTemplateString)
	var queryBytes bytes.Buffer
	queryTemplate.Execute(&queryBytes, query)

	if db.Debug {
		fmt.Println("Query String: ", queryBytes.String())
	}
	if err == nil {
		return queryBytes.String()
	}
	return "Failed to generate query"
}

// Queries
${this.printOperation(queryFields, 'query')}

// Mutations
${this.printOperation(mutationFields, 'mutation')}

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

      // GraphQL Send a GraphQL operation request
// TODO: arg variables can be made optional via variadic func approach
func (db DB) GraphQL(query string, variables map[string]interface{}) map[string]interface{} {
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
