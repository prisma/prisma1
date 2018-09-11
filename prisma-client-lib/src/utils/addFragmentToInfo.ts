import {
  GraphQLResolveInfo,
  GraphQLScalarType,
  parse,
  FragmentDefinitionNode,
  OperationDefinitionNode,
} from 'graphql'
import { getDeepType } from '../info'
import * as immutable from 'object-path-immutable'

export function addFragmentToInfo(
  info: GraphQLResolveInfo,
  fragment: string,
): GraphQLResolveInfo {
  const returnType = getDeepType(info.returnType)
  if (returnType instanceof GraphQLScalarType) {
    throw new Error(
      `Can't add fragment "${fragment}" because return type of info object is a scalar type ${info.returnType.toString()}`,
    )
  }

  const ast = parse(fragment)

  const deepReturnType = getDeepType(returnType)

  if (
    ast.definitions[0].kind === 'FragmentDefinition' &&
    (ast.definitions[0] as FragmentDefinitionNode).typeCondition.name.value !==
      deepReturnType.toString()
  ) {
    throw new Error(
      `Type ${
        (ast.definitions[0] as FragmentDefinitionNode).typeCondition.name.value
      } specified in fragment doesn't match return type ${deepReturnType.toString()}`,
    )
  }

  return (immutable as any).update(
    info,
    ['fieldNodes', 0, 'selectionSet', 'selections'],
    selections =>
      selections.concat(
        (ast.definitions[0] as OperationDefinitionNode).selectionSet.selections,
      ),
  )
}
