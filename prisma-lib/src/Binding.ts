import {
  QueryMap,
  BindingOptions,
  SubscriptionMap /*, Operation*/,
  QueryOrMutation,
} from './types'
import {
  GraphQLObjectType,
  GraphQLScalarType,
  Kind,
  OperationTypeNode,
  print,
  execute,
  GraphQLField,
} from 'graphql'
import { Delegate } from './Delegate'
import { mapValues } from './utils/mapValues'
const log = require('debug')('binding')
// to make the TS compiler happy

// to avoid recreation on each instantiation for the same schema, we cache the created methods
const delegateCache = new Map()

let instructionId = 0

export interface InstructionsMap {
  [key: string]: Instruction[]
}

export interface Instruction {
  fieldName: string
  args?: any
  field: GraphQLField<any, any>
}

export class Binding extends Delegate {
  subscription: SubscriptionMap
  types: any
  query: any
  debug
  mutation: any
  currentInstructions: InstructionsMap = {}

  constructor({ schema, fragmentReplacements, before, debug }: BindingOptions) {
    super({ schema, fragmentReplacements, before })
    this.debug = debug

    this.buildMethods()
    this.subscription = this.buildSubscriptionMethods()
  }

  buildQueryMethods(operation: QueryOrMutation): QueryMap {
    const queryType =
      operation === 'query'
        ? this.schema.getQueryType()
        : this.schema.getMutationType()
    if (!queryType) {
      return {}
    }
    const fields = queryType.getFields()
    return Object.entries(fields)
      .map(([fieldName, field]) => {
        return {
          key: fieldName,
          value: (args, info, options) => {
            return this.delegate(operation, fieldName, args, info, options)
          },
        }
      })
      .reduce((acc, curr) => ({ ...acc, [curr.key]: curr.value }), {})
  }

  buildSubscriptionMethods(): SubscriptionMap {
    const subscriptionType = this.schema.getSubscriptionType()
    if (!subscriptionType) {
      return {}
    }
    const fields = subscriptionType.getFields()
    return Object.entries(fields)
      .map(([fieldName, field]) => {
        return {
          key: fieldName,
          value: (args, info, options) => {
            return this.delegateSubscription(fieldName, args, info, options)
          },
        }
      })
      .reduce((acc, curr) => ({ ...acc, [curr.key]: curr.value }), {})
  }

  getOperation(instructions) {
    const topLevelQuery = instructions[0].fieldName
    const query = this.schema.getQueryType()!.getFields()
    if (query[topLevelQuery]) {
      return 'query'
    }

    return 'mutation'
  }

  processInstructions = async (id: number): Promise<any> => {
    log('process instructions')
    const instructions = this.currentInstructions[id]

    const { ast, variables } = this.generateSelections(instructions)
    log('generated selections')
    const { variableDefinitions, ...restAst } = ast
    const document = {
      kind: Kind.DOCUMENT,
      definitions: [
        {
          kind: Kind.OPERATION_DEFINITION,
          operation: this.getOperation(instructions) as OperationTypeNode,
          directives: [],
          variableDefinitions,
          selectionSet: {
            kind: Kind.SELECTION_SET,
            selections: [restAst],
          },
        },
      ],
    }
    this.before()
    if (this.debug) {
      console.log(`\nQuery:`)
      const query = print(document)
      console.log(query)
      if (variables && Object.keys(variables).length > 0) {
        console.log('Variables:')
        console.log(JSON.stringify(variables))
      }
    }

    log('printed / before')
    const result = await this.execute(document, variables)
    log('executed')
    let pointer = result
    let count = 0
    while (
      pointer &&
      typeof pointer === 'object' &&
      !Array.isArray(pointer) &&
      count++ < instructions.length
    ) {
      pointer = pointer[Object.keys(pointer)[0]]
    }
    log('unpack it')
    return pointer
  }

  execute(document, variables) {
    return execute(this.schema, document, {}, {}, variables) as any
  }

  then = async (id, resolve, reject) => {
    let result
    try {
      // const before = Date.now()
      result = await this.processInstructions(id)
      // console.log(`then: ${Date.now() - before}`)
      this.currentInstructions[id] = []
      resolve(result)
    } catch (e) {
      this.currentInstructions[id] = []
      reject(e)
    }
    return result
  }

  catch = async (id, reject) => {
    try {
      await this.processInstructions(id)
    } catch (e) {
      this.currentInstructions[id] = []
      reject(e)
    }
  }

  generateSelections(instructions) {
    const variableDefinitions: any[] = []
    const variables = {}
    let variableCounter = {}

    const ast = instructions.reduceRight((acc, instruction, index) => {
      let args: any[] = []

      if (instruction.args && Object.keys(instruction.args).length > 0) {
        Object.entries(instruction.args).forEach(([name, value]) => {
          let variableName
          if (typeof variableCounter[name] === 'undefined') {
            variableName = name
            variableCounter[name] = 0
          } else {
            variableCounter[name]++
            variableName = `${name}_${variableCounter[name]}`
          }
          variables[variableName] = value
          const inputArg = instruction.field.args.find(arg => arg.name === name)
          if (!inputArg) {
            throw new Error(
              `Could not find argument ${name} for type ${this.getTypeName(
                instruction.field.type,
              )}`,
            )
          }

          variableDefinitions.push({
            kind: Kind.VARIABLE_DEFINITION,
            variable: {
              kind: Kind.VARIABLE,
              name: {
                kind: Kind.NAME,
                value: variableName,
              },
            },
            type: inputArg.astNode.type,
          })

          args.push({
            kind: Kind.ARGUMENT,
            name: {
              kind: Kind.NAME,
              value: name,
            },
            value: {
              kind: Kind.VARIABLE,
              name: {
                kind: 'Name',
                value: variableName,
              },
            },
          })
        })
      }

      const node = {
        kind: Kind.FIELD,
        name: {
          kind: Kind.NAME,
          value: instruction.fieldName,
        },
        arguments: args,
        directives: [],
        selectionSet: {
          kind: Kind.SELECTION_SET,
          selections: [] as any[],
        },
      }

      const type = this.getDeepType(instruction.field.type)
      if (
        index === instructions.length - 1 &&
        type instanceof GraphQLObjectType
      ) {
        node.selectionSet.selections = Object.entries(type.getFields())
          .filter(([_, field]: any) => {
            const fieldType = this.getDeepType(field.type)
            return fieldType instanceof GraphQLScalarType
          })
          .map(([fieldName]) => ({
            kind: Kind.FIELD,
            name: {
              kind: Kind.NAME,
              value: fieldName,
            },
            arguments: [],
            directives: [],
          }))
      }

      if (acc) {
        node.selectionSet.selections.push(acc)
      }

      return node
    }, null)

    return {
      ast: { ...ast, variableDefinitions },
      variables,
    }
  }

  buildMethods() {
    this.buildDelegateMethods()
    this.buildORMMethods()
  }

  buildDelegateMethods() {
    const methods = this.getDelegateMethods()
    Object.assign(this.delegate, methods)
  }

  getDelegateMethods() {
    const cachedMethods = delegateCache.get(this.schema)
    if (cachedMethods) {
      return cachedMethods
    }
    const methods = {
      query: this.buildQueryMethods('query'),
      mutation: this.buildQueryMethods('mutation'),
      subscription: this.buildSubscriptionMethods(),
    }
    delegateCache.set(this.schema, methods)
    return methods
  }

  buildORMMethods() {
    this.types = this.getORMTypes()
    this.query = this.types.Query
    this.mutation = this.types.Mutation
  }

  getORMTypes() {
    const typeMap = this.schema.getTypeMap()
    const types = Object.entries(typeMap)
      .map(([name, type]) => {
        let value = {
          then: this.then,
          catch: this.catch,
          [Symbol.toStringTag]: 'Promise',
        }
        if (type instanceof GraphQLObjectType) {
          value = {
            ...value,
            ...Object.entries(type.getFields())
              .map(([fieldName, field]) => {
                return {
                  key: fieldName,
                  value: (args, arg2) => {
                    const id = typeof args === 'number' ? args : ++instructionId

                    const realArgs = typeof args === 'number' ? arg2 : args
                    this.currentInstructions[id] =
                      this.currentInstructions[id] || []
                    this.currentInstructions[id].push({
                      fieldName,
                      args: realArgs,
                      field,
                    })
                    const typeName = this.getTypeName(field.type)

                    // this is black magic. what we do here: bind both .then, .catch and all resolvers to `id`
                    return mapValues(this.types[typeName], (key, value) => {
                      if (typeof value === 'function') {
                        return value.bind(this, id)
                      }
                      return value
                    })
                  },
                }
              })
              .reduce(reduceKeyValue, {}),
          }
        }

        return {
          key: name,
          value,
        }
      })
      .reduce(reduceKeyValue, {})

    return types
  }

  getTypeName(type): string {
    if (type.ofType) {
      return this.getDeepType(type.ofType)
    }
    return type.name
  }

  getDeepType(type) {
    if (type.ofType) {
      return this.getDeepType(type.ofType)
    }

    return type
  }
}

const reduceKeyValue = (acc, curr) => ({
  ...acc,
  [curr.key]: curr.value,
})
