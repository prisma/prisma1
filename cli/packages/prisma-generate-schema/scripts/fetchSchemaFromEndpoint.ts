import * as popsicle from 'popsicle'
import { introspectionQuery, buildClientSchema, printSchema } from 'graphql/utilities'

if(process.argv.length < 3) {
  console.error("Usage: ts-node fetschSchemaFromEndpoint.ts ENDPOINT")
  process.exit(1)
}

const endpoint = process.argv[2]

popsicle.post({
  url: endpoint,
  body: {
    query: introspectionQuery
  }
}).then(response => {
  const schema = JSON.parse(response.body).data
  console.log(printSchema(buildClientSchema(schema)))
}).catch(error => {
  console.error(error)
})