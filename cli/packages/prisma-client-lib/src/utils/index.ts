import {
  isWrappingType,
  isListType,
  GraphQLInputObjectType,
  GraphQLObjectType,
  GraphQLScalarType,
  GraphQLInterfaceType,
  GraphQLUnionType,
  GraphQLList,
  GraphQLEnumType,
  GraphQLNonNull,
  GraphQLSchema,
  GraphQLResolveInfo,
  GraphQLOutputType,
  print,
} from 'graphql'

import { Operation } from '../types'

export function isScalar(t: GraphQLOutputType): boolean {
  console.log(
    { typeof: typeof t },
    { constructor: t.constructor.name },
    { isObject: t instanceof Object },
  )
  if (t instanceof GraphQLScalarType || t instanceof GraphQLEnumType) {
    return true
  }

  if (
    t instanceof GraphQLObjectType ||
    t instanceof GraphQLInterfaceType ||
    t instanceof GraphQLUnionType ||
    t instanceof GraphQLList
  ) {
    return false
  }

  const nnt = t as any
  if (nnt instanceof GraphQLNonNull) {
    if (
      nnt.ofType instanceof GraphQLScalarType ||
      nnt.ofType instanceof GraphQLEnumType
    ) {
      return true
    }
  }

  return false
}

export function getTypeForRootFieldName(
  rootFieldName: string,
  operation: Operation,
  schema: GraphQLSchema,
): GraphQLOutputType {
  if (operation === 'mutation' && !schema.getMutationType()) {
    throw new Error(`Schema doesn't have mutation type`)
  }

  if (operation === 'subscription' && !schema.getSubscriptionType()) {
    throw new Error(`Schema doesn't have subscription type`)
  }

  const rootType =
    {
      query: () => schema.getQueryType(),
      mutation: () => schema.getMutationType()!,
      subscription: () => schema.getSubscriptionType()!,
    }[operation]() || undefined!

  const rootField = rootType.getFields()[rootFieldName]

  if (!rootField) {
    throw new Error(`No such root field found: ${rootFieldName}`)
  }

  return rootField.type
}

export function printDocumentFromInfo(info: GraphQLResolveInfo) {
  const fragments = Object.keys(info.fragments).map(
    fragment => info.fragments[fragment],
  )
  const doc = {
    kind: 'Document',
    definitions: [
      {
        kind: 'OperationDefinition',
        operation: 'query',
        selectionSet: info.fieldNodes[0].selectionSet,
      },
      ...fragments,
    ],
  }

  return print(doc)
}

function lowerCaseFirst(str) {
  return str[0].toLowerCase() + str.slice(1)
}

export function getExistsTypes(queryType: GraphQLObjectType) {
  const types = getTypesAndWhere(queryType)
  return types
    .map(
      ({ type, where }) =>
        `  ${lowerCaseFirst(type)}: (where?: ${where}) => Promise<boolean>`,
    )
    .join('\n')
}

export function getExistsFlowTypes(queryType: GraphQLObjectType) {
  const types = getTypesAndWhere(queryType)
  return types
    .map(
      ({ type, where }) =>
        `${lowerCaseFirst(type)}(where?: ${where}): Promise<boolean>;`,
    )
    .join('\n')
}

export function getTypesAndWhere(queryType: GraphQLObjectType) {
  const fields = queryType.getFields()
  const listFields = Object.keys(fields).reduce(
    (acc, field) => {
      const deepType = getDeepListType(fields[field])
      if (deepType) {
        acc.push({ field: fields[field], deepType })
      }
      return acc
    },
    [] as any[],
  )

  return listFields.map(({ field, deepType }) => ({
    type: deepType.name,
    pluralFieldName: field.name,
    where: getWhere(field),
  }))
}

export function getWhere(field) {
  return (field.args.find(a => a.name === 'where')!
    .type as GraphQLInputObjectType).name
}

export function getDeepType(type) {
  if (type.ofType) {
    return getDeepType(type.ofType)
  }

  return type
}

export function getDeepListType(field) {
  const type = field.type
  if (isListType(type)) {
    return type.ofType
  }

  if (isWrappingType(type) && isListType(type.ofType)) {
    return type.ofType.ofType
  }

  return null
}
