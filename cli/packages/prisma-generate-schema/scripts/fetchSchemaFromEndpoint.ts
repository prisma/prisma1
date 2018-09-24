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
  })
  .then(response => {
    const schema = JSON.parse(response.body).data
    const sdl = printSchema(buildClientSchema(schema))
    const parsedSdl = parse(sdl)
    const mutatedSdl = visit(parsedSdl, {
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
