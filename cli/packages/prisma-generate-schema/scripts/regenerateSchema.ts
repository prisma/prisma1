import * as fs from 'fs'
import * as path from 'path'
import * as fetch from 'node-fetch'

const testNames = fs.readdirSync(
  path.join(__dirname, '../__tests__/blackbox/cases'),
)

async function deployModel(service, stage, datamodel) {
  await fetch(`http://localhost:4466/management`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      query: `mutation {
      addProject(input: {
        name: "${service}"
        stage: "${stage}"
      }) {
        clientMutationId
      }
    }`,
    }),
  })

  const variables = {
    input: {
      name: service,
      stage,
      types: datamodel,
    },
  }

  await fetch(`http://localhost:4466/management`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      query: `mutation ($input: DeployInput!) {
        deploy(input: $input) {
          clientMutationId
        }
      }`,
      variables,
    }),
  })
}
