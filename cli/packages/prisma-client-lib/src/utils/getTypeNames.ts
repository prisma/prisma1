import { GraphQLSchema } from 'graphql'

// TODO: Remove the same function from typescript-generator and use this one instead. 
export function getTypeNames(ast: GraphQLSchema) {
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
