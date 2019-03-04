import { GraphQLScalarType, GraphQLEnumType } from 'graphql'

import { getDeepType } from '.'

function isScalar(field) {
  const fieldType = getDeepType(field.type)

  return (
    fieldType instanceof GraphQLScalarType ||
    fieldType instanceof GraphQLEnumType
  )
}

export function connectionNodeHasScalars({ type }) {
  const edgesField: any[] = Object.values(type.getFields()).filter(
    (subField: any) => {
      return subField.name === 'edges'
    },
  )
  if (edgesField.length === 0) {
    return false
  }

  const edgesFieldType = getDeepType(edgesField[0].type)
  const nodeField: any[] = Object.values(edgesFieldType.getFields()).filter(
    (subField: any) => {
      return subField.name === 'node'
    },
  )
  if (nodeField.length === 0) {
    return false
  }
  const nodeFieldType = getDeepType(nodeField[0].type)
  const nodeFieldScalars: any[] = Object.values(
    nodeFieldType.getFields(),
  ).filter(subField => {
    return isScalar(subField)
  })
  return nodeFieldScalars.length > 0
}
