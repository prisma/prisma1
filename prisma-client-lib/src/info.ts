import {
  GraphQLSchema,
  GraphQLResolveInfo,
  FieldNode,
  SelectionSetNode,
  GraphQLOutputType,
  GraphQLObjectType as GraphQLObjectTypeRef,
  GraphQLScalarType as GraphQLScalarTypeRef,
  getNamedType,
  DocumentNode,
  print,
  isNonNullType,
  isListType,
  isScalarType,
  isObjectType,
  parse,
  validate,
  Kind,
} from 'graphql'

import { Operation } from './types'
import { isScalar, getTypeForRootFieldName } from './utils'
import { addFragmentToInfo } from './utils/addFragmentToInfo'

export function buildInfo(
  rootFieldName: string,
  operation: Operation,
  schema: GraphQLSchema,
  info?: GraphQLResolveInfo | string | DocumentNode,
): GraphQLResolveInfo {
  if (!info) {
    info = buildInfoForAllScalars(rootFieldName, schema, operation)
  } else if ((info as any).kind && (info as any).kind === 'Document') {
    info = print(info)
  }
  if (typeof info === 'string') {
    info = buildInfoFromFragment(rootFieldName, schema, operation, info)
  }
  return info as any
}

export function buildInfoForAllScalars(
  rootFieldName: string,
  schema: GraphQLSchema,
  operation: Operation,
): GraphQLResolveInfo {
  const fieldNodes: FieldNode[] = []
  const type = getTypeForRootFieldName(rootFieldName, operation, schema)
  const namedType = getNamedType(type)

  let selections: FieldNode[] | undefined
  if (isObjectType(namedType)) {
    const fields = (namedType as any).getFields()
    selections = Object.keys(fields)
      .filter(f => isScalar(fields[f].type))
      .map<FieldNode>(fieldName => {
        const field = fields[fieldName]
        return {
          kind: 'Field',
          name: { kind: 'Name', value: field.name },
        }
      })
  }

  const fieldNode: FieldNode = {
    kind: 'Field',
    name: { kind: 'Name', value: rootFieldName },
    selectionSet: selections ? { kind: 'SelectionSet', selections } : undefined,
  }

  fieldNodes.push(fieldNode)

  const parentType =
    {
      query: () => schema.getQueryType(),
      mutation: () => schema.getMutationType()!,
      subscription: () => schema.getSubscriptionType()!,
    }[operation]() || undefined

  return {
    fieldNodes,
    fragments: {},
    schema,
    fieldName: rootFieldName,
    returnType: type,
    parentType: parentType!,
    path: undefined!,
    rootValue: undefined,
    operation: {
      kind: 'OperationDefinition',
      operation,
      selectionSet: { kind: 'SelectionSet', selections: [] },
      variableDefinitions: [],
    },
    variableValues: {},
  }
}

export function buildInfoFromFragment(
  rootFieldName: string,
  schema: GraphQLSchema,
  operation: Operation,
  query: string,
): GraphQLResolveInfo {
  const type = getTypeForRootFieldName(rootFieldName, operation, schema)
  const namedType = getNamedType(type)!
  const fieldNode: FieldNode = {
    kind: 'Field',
    name: { kind: 'Name', value: rootFieldName },
    selectionSet: extractQuerySelectionSet(query, namedType.name!, schema),
  }

  return {
    fieldNodes: [fieldNode],
    fragments: {},
    schema,
    fieldName: rootFieldName,
    returnType: type,
    parentType: schema.getQueryType() || undefined!,
    path: undefined!,
    rootValue: undefined,
    operation: {
      kind: 'OperationDefinition',
      operation,
      selectionSet: { kind: 'SelectionSet', selections: [] },
      variableDefinitions: [],
    },
    variableValues: {},
  }
}

function extractQuerySelectionSet(
  query: string,
  typeName: string,
  schema: GraphQLSchema,
): SelectionSetNode {
  if (!query.startsWith('fragment')) {
    query = `fragment tmp on ${typeName} ${query}`
  }
  const document = parse(query)
  const errors = validate(schema, document).filter(
    e => e.message.match(/Fragment ".*" is never used./) === null,
  )
  if (errors.length > 0) {
    throw errors
  }

  const queryNode = document.definitions[0]
  if (!queryNode || queryNode.kind !== 'FragmentDefinition') {
    throw new Error(`Invalid query: ${query}`)
  }

  return queryNode.selectionSet
}

/**
 * Generates a sub info based on the provided path. If provided path is not included in the selection set, the function returns null.
 * @param info GraphQLResolveInfo
 * @param path string
 * @param fragment string | undefined
 */
export function makeSubInfo(
  info: GraphQLResolveInfo,
  path: string,
  fragment?: string,
): GraphQLResolveInfo | null {
  const returnType = getDeepType(info.returnType)
  if (isScalarType(returnType)) {
    throw new Error(`Can't make subInfo for type ${info.returnType.toString()}`)
  }

  const splittedPath = path.split('.')
  const fieldsToTraverse = splittedPath.slice()
  let currentType = info.returnType
  let currentSelectionSet = info.fieldNodes[0].selectionSet!
  let currentFieldName
  let parentType
  let currentPath = info.path || {
    prev: undefined,
    key: info.fieldNodes[0].name.value,
  }

  while (fieldsToTraverse.length > 0) {
    currentFieldName = fieldsToTraverse.shift()!
    if (!isObjectType(currentType)) {
      throw new Error(
        `Can't get subInfo for type ${currentType.toString()} as needs to be a GraphQLObjectType`,
      )
    }

    const fields = (currentType as any).getFields()
    if (!fields[currentFieldName]) {
      throw new Error(
        `Type ${currentType.toString()} has no field called ${currentFieldName}`,
      )
    }

    const currentFieldType = fields[currentFieldName].type
    if (!isObjectType(currentFieldType)) {
      throw new Error(
        `Can't get subInfo for type ${currentFieldType} of field ${currentFieldName} on type ${currentType.toString()}`,
      )
    }
    parentType = currentType
    currentType = currentFieldType
    let suitableSelection = currentSelectionSet.selections!.find(
      selection =>
        selection.kind === 'Field' && selection.name.value === currentFieldName,
    )

    if (!suitableSelection) {
      // if there is no field selection, there still could be fragments
      currentSelectionSet = currentSelectionSet.selections.reduce(
        (acc: any, curr) => {
          if (acc) {
            return acc
          }
          if (curr.kind === 'InlineFragment') {
            return curr.selectionSet
          }
        },
        null,
      )!
    } else if (suitableSelection.kind === 'Field') {
      currentSelectionSet = suitableSelection.selectionSet!
    }

    if (!currentSelectionSet) {
      return null
    }

    currentPath = addPath(currentPath, currentFieldName)
  }

  const fieldNode: FieldNode = {
    kind: 'Field',
    name: { kind: 'Name', value: currentFieldName },
    selectionSet: currentSelectionSet,
  }

  const newInfo = {
    fieldNodes: [fieldNode],
    fragments: {},
    schema: info.schema,
    fieldName: currentFieldName,
    returnType: currentType,
    parentType,
    path: currentPath,
    rootValue: undefined,
    operation: {
      kind: Kind.OPERATION_DEFINITION,
      operation: currentFieldName,
      selectionSet: { kind: Kind.SELECTION_SET, selections: [] },
      variableDefinitions: [],
    },
    variableValues: {},
  }

  if (fragment) {
    return addFragmentToInfo(newInfo, fragment)
  }

  return newInfo
}

export function getDeepType(
  type: GraphQLOutputType,
): GraphQLObjectTypeRef | GraphQLScalarTypeRef {
  if ((type as any).ofType) {
    return getDeepType((type as any).ofType)
  }

  return type as any
}

export function addPath(prev, key) {
  return { prev, key }
}

export function buildExistsInfo(
  rootFieldName: string,
  schema: GraphQLSchema,
): GraphQLResolveInfo {
  const queryType = schema.getQueryType() || undefined!
  const type = queryType.getFields()[rootFieldName].type

  // make sure that just list types are queried
  if (!isNonNullType(type) || !isListType(type)) {
    throw new Error(`Invalid exist query: ${rootFieldName}`)
  }

  const fieldNode: FieldNode = {
    kind: 'Field',
    name: { kind: 'Name', value: rootFieldName },
    selectionSet: {
      kind: 'SelectionSet',
      selections: [
        {
          kind: 'Field',
          name: { kind: 'Name', value: 'id' },
        },
      ],
    },
  }

  return {
    fieldNodes: [fieldNode],
    fragments: {},
    schema,
    fieldName: rootFieldName,
    returnType: type,
    parentType: queryType,
    path: undefined!,
    rootValue: null,
    operation: {
      kind: 'OperationDefinition',
      operation: 'query',
      selectionSet: { kind: 'SelectionSet', selections: [] },
      variableDefinitions: [],
    },
    variableValues: {},
  }
}
