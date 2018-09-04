import { FragmentReplacement } from './types'
import { IResolvers } from 'graphql-tools/dist/Interfaces'

export function extractFragmentReplacements(
  resolvers: IResolvers,
): FragmentReplacement[] {
  const fragmentReplacements: FragmentReplacement[] = []

  for (const typeName in resolvers) {
    const fieldResolvers: any = resolvers[typeName]
    for (const fieldName in fieldResolvers) {
      const fieldResolver = fieldResolvers[fieldName]
      if (typeof fieldResolver === 'object' && fieldResolver.fragment) {
        fragmentReplacements.push({
          field: fieldName,
          fragment: fieldResolver.fragment,
        })
      }
    }
  }

  return fragmentReplacements
}
