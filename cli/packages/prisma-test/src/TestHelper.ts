import { getTmpDir } from '../../prisma-cli-core/src/test/getTmpDir'
import * as fs from 'fs-extra'
import * as path from 'path'
import * as cuid from 'scuid'
import { request, GraphQLClient } from 'graphql-request'

export enum REGION {
  EU = 'eu',
  US = 'us',
}

export type MockEnvironment = {
  region: REGION | undefined
  cwd?: string | undefined
  endpoint?: string | undefined
}

export function setupMockEnvironment(
  environment: MockEnvironment,
): MockEnvironment {
  const { region } = environment
  const home = getTmpDir()
  const cwd = environment.cwd ? environment.cwd : path.join(home, cuid())
  fs.mkdirpSync(cwd)
  fs.writeFileSync(
    `${cwd}/datamodel.graphql`,
    `
      type User {
        id: ID! @unique
        name: String!
      }
    `,
  )
  // const endpoint = `https://${region}1.prisma.sh/public-${cuid()}/test/test`
  const endpoint = `http://localhost:4466/test-${cuid()}/test`
  fs.writeFileSync(
    `${cwd}/prisma.yml`,
    `
      endpoint: ${endpoint}
      datamodel: datamodel.graphql
    `,
  )

  return {
    region,
    cwd,
    endpoint,
  }
}

export async function getUserToken(api, email, password, name) {
  const queryForUserToken = `
      mutation LoginToken {
        authenticateWithEmail(
          input: { email: "${email}", password: "${password}", name: "${name}" }
        ) {
          token
        }
      }
    `
  const userToken = ((await request(api, queryForUserToken)) as any)
    .authenticateWithEmail.token

  return userToken
}

export async function getWorkspaceSlug(api, userToken) {
  const queryForWorkspaceSlug = `
      query WorkspaceSlug {
        me {
          memberships {
            workspace {
              slug
            }
          }
        }
      }
    `
  const cloudClient = getCloudClient(api, userToken)
  const workspaceSlug = ((await cloudClient.request(
    queryForWorkspaceSlug,
  )) as any).me.memberships[0].workspace.slug

  return workspaceSlug
}

export async function getClusterToken(
  api,
  workspace,
  region,
  service,
  stage,
  userToken,
) {
  const queryForClusterToken = `
      mutation ClusterToken {
        generateClusterToken(
          input: {
            workspaceSlug: "${workspace}"
            clusterName: "${region}"
            serviceName: "${service}"
            stageName: "${stage}"
          }
        ) {
          clusterToken
        }
      }
    `
  const cloudClient = getCloudClient(api, userToken)
  const clusterToken = ((await cloudClient.request(
    queryForClusterToken,
  )) as any).generateClusterToken.clusterToken
  return clusterToken
}

export async function addProject(api, workspace, service, stage, clusterToken) {
  const clusterClient = getClusterClient(api, clusterToken)

  const queryToAddProject = `
    mutation AddProject {
      addProject(
        input: {
          name: "${workspace}~${service}"
          stage: "${stage}"
        }
      ) {
        project {
          metricKey
          name
          stage
        }
      }
    }
  `
  const projectPayload = ((await clusterClient.request(
    queryToAddProject,
  )) as any).addProject
  return projectPayload
}

export async function deployProject(
  api,
  workspace,
  service,
  stage,
  clusterToken,
) {
  const clusterClient = getClusterClient(api, clusterToken)
  const queryToDeployProject = `
      mutation Deploy {
        deploy(
          input: {
            name: "${workspace}~${service}"
            stage: "${stage}"
            types: "type User { id: ID! @unique text: String }"
          }
        ) {
          errors {
            description
          }
          migration {
            revision
          }
        }
      }
    `

  const deployProjectPayload = ((await clusterClient.request(
    queryToDeployProject,
  )) as any).deploy
  return deployProjectPayload
}

export async function deleteProject(
  api,
  workspace,
  service,
  stage,
  clusterToken,
) {
  const clusterClient = getClusterClient(api, clusterToken)
  const queryToDeleteProject = `
      mutation DeleteProject {
        deleteProject(input: { name: "${workspace}~${service}", stage: "${stage}" }) {
          project {
            metricKey
            name
            stage
          }
        }
      }
    `

  const deletedProjectPayload = ((await clusterClient.request(
    queryToDeleteProject,
  )) as any).deleteProject
  return deletedProjectPayload
}

function getCloudClient(api, userToken) {
  return new GraphQLClient(api, {
    headers: {
      Authorization: `Bearer ${userToken}`,
    },
  })
}

function getClusterClient(api, clusterToken) {
  return new GraphQLClient(api, {
    headers: {
      Authorization: `Bearer ${clusterToken}`,
    },
  })
}
