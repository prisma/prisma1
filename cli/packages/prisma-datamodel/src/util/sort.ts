import { IGQLType } from "../datamodel/model";
import GQLAssert from "./gqlAssert";

/**
 * Sorts the given list of types in topological order, 
 * depending on their relation. Respects embedded types.
 * 
 * The topological order is established by traversing the dependency graph in depth-first order (dfs toposort).
 * @param types The types to sort.
 * @returns A new list containing all types in topological order.
 */
export function toposort(types: IGQLType[]) {
  const sorted: IGQLType[] = []

  for(const type of types) {
    // Embedded never at top level. 
    if(!type.isEmbedded) {
      toposortType(type, sorted)
    }
  }

  if(sorted.length !== types.length) {
    GQLAssert.raise('Failed to establish topological order. This usually indicates an embedded type which is not used in the data model.')
  }

  return sorted
}

function toposortType(type: IGQLType, sorted: IGQLType[]) {
  if(!sorted.includes(type)) {
    sorted.push(type)

    for(const field of type.fields) {
      if(typeof field.type !== 'string') {
        toposortType(field.type, sorted)
      }
    }
  }
}