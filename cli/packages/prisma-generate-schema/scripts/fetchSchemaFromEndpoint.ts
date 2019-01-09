import * as popsicle from 'popsicle'
import {
  introspectionQuery,
  buildClientSchema,
  printSchema,
} from 'graphql/utilities'
import {
  parse,
  visit,
  print,
  EnumTypeDefinitionNode,
  FieldDefinitionNode,
  DefinitionNode,
  NamedTypeNode,
  ObjectTypeDefinitionNode,
  isNamedType,
  isScalarType,
  isListType,
} from 'graphql'

if (process.argv.length < 3) {
  console.error('Usage: ts-node fetschSchemaFromEndpoint.ts ENDPOINT')
  process.exit(1)
}

const endpoint = process.argv[2]

popsicle
  .post({
    url: endpoint,
    body: {
      query: introspectionQuery,
    },
    transport: popsicle.createTransport({
      type: 'text',
      maxBufferSize: 500 * 1024 * 1024, // 200 MB Max Buffer
    }),
  })
  .then(response => {
    const schema = JSON.parse(response.body).data
    const sdl = printSchema(buildClientSchema(schema))
    const parsedSdl = parse(sdl)
    const mutatedSdl = visit(parsedSdl, {
      ObjectTypeDefinition: {
        enter(node: ObjectTypeDefinitionNode) {
          if (
            !['Query', 'Mutation', 'Subscription'].includes(node.name.value)
          ) {
            const nodeWithValidFields = visit(node, {
              FieldDefinition: {
                enter: (fieldNode: FieldDefinitionNode) => {
                  if (
                    (fieldNode.arguments &&
                      fieldNode.arguments.length > 0 &&
                      fieldNode.type.kind === 'NamedType') ||
                    (fieldNode.type.kind === 'NonNullType' &&
                      !isScalarType(fieldNode.type))
                  ) {
                    return {
                      ...fieldNode,
                      arguments: fieldNode.arguments
                        ? fieldNode.arguments.filter(
                            arg => arg.name.value !== 'where',
                          )
                        : [],
                    }
                  } else {
                    return fieldNode
                  }
                },
              },
            })
            return nodeWithValidFields
          }
        },
      },
      EnumTypeDefinition: {
        enter(enumNode: EnumTypeDefinitionNode) {
          if (enumNode.name.value === 'PrismaDatabase') {
            return null
          }
        },
      },
      FieldDefinition: {
        enter(fieldNode: FieldDefinitionNode) {
          if (fieldNode.name.value === 'executeRaw') {
            return null
          }
        },
      },
    })
    console.log(print(mutatedSdl))
  })
  .catch(error => {
    console.error(error)
  })
