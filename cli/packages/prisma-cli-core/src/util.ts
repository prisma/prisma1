import { Region } from 'prisma-cli-engine'
import chalk from 'chalk'
import { Cluster } from 'prisma-yml'

const devPrefix = process.env.ENV === 'DEV' ? 'dev.' : ''

export const consoleURL = (token: string, projectName?: string) =>
  `https://${devPrefix}console.graph.cool/token?token=${token}${
    projectName ? `&redirect=/${encodeURIComponent(projectName)}` : ''
  }`
// export const playgroundURL = (token: string, projectName: string) =>

export function sortByTimestamp(a, b) {
  return a.timestamp < b.timestamp ? -1 : 1
}

/**
 * Print a list of [['key', 'value'],...] pairs properly padded
 * @param {string[][]} arr1
 * @param {number} spaceLeft
 * @param {number} spaceBetween
 */

export const prettyProject = p => `${chalk.bold(p.name)} (${p.id})`

export function prettyTime(time: number): string {
  const output =
    time > 1000 ? (Math.round(time / 100) / 10).toFixed(1) + 's' : time + 'ms'
  return chalk.cyan(output)
}

export function concatName(
  cluster: Cluster,
  name: string,
  workspace: string | null,
) {
  if (cluster.shared) {
    const workspaceString = workspace ? `${workspace}~` : ''
    return `${workspaceString}${name}`
  }

  return name
}

export const defaultDataModel = `\
type User {
  id: ID! @unique
  name: String!
}
`

export const defaultDockerCompose = `\
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:experimental
    restart: always
    ports:
    - "4466:4466"
    environment:
      CLUSTER_ADDRESS: ""
      SCHEMA_MANAGER_SECRET: ""
      SCHEMA_MANAGER_ENDPOINT: ""
      BUGSNAG_API_KEY: ""
      PRISMA_CONFIG: |
        port: 4466
`

// legacySecret: old public key
// managementApiSecret: somesecret

//
//     databases:
//       default:
//         connector: mysql
//         active: true
//         host: your-mysql-host
//         port: 3306
//         user: someuser
//         password: existingpw
