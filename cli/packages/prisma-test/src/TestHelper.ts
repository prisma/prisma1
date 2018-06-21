import { getTmpDir } from '../../prisma-cli-core/src/test/getTmpDir'
import * as fs from 'fs-extra'
import * as path from 'path'
import * as cuid from 'scuid'

export enum REGION {
  EU = 'eu',
  US = 'us',
}

export type MockEnvironment = {
  region: REGION | undefined
  cwd?: string | undefined
  endpoint?: string | undefined
}

export function setupMockEnvironment(environment: MockEnvironment): MockEnvironment {
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
