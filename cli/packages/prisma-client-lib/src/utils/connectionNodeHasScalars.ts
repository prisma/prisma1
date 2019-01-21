import {
    GraphQLScalarType,
    GraphQLEnumType
} from 'graphql'

import { getDeepType } from '.'

function isScalar(field) {
    const fieldType = getDeepType(field.type)

    return (
        fieldType instanceof GraphQLScalarType ||
        fieldType instanceof GraphQLEnumType
    )
}

export function connectionNodeHasScalars({ type }) {
    const edgesField = Object.entries(type.getFields())
        .filter(([, subField]: any) => {
            return subField.name === 'edges'
        })
        .map(([, subField]: any) => {
            return subField
        })
    if (edgesField.length === 0) {
        return false
    }

    const edgesFieldType = getDeepType(edgesField[0].type)
    const nodeField = Object.entries(edgesFieldType.getFields())
        .filter(([, subField]: any) => {
            return subField.name === 'node'
        })
        .map(([, subField]: any) => {
            return subField
        })
    if (nodeField.length === 0) {
        return false
    }
    const nodeFieldType = getDeepType(nodeField[0].type)
    const nodeFieldScalars = Object.entries(nodeFieldType.getFields())
        .filter(([, subField]: any) => {
            return isScalar(subField)
        })
        .map(([, subField]: any) => {
            return subField
        })
    return nodeFieldScalars.length > 0
}