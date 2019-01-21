import { IntrospectionQuery } from 'graphql'

export function hasTypeWithField(
  introspection: IntrospectionQuery,
  type: string,
  field: string,
) {
  const foundType = introspection.__schema.types.find(t => t.name === type)
  if (foundType) {
    if (foundType.kind === 'OBJECT') {
      return foundType.fields.some(f => f.name === field)
    }
  }

  return false
}
