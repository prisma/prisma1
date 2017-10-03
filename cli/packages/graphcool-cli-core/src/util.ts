import { Region } from 'graphcool-cli-engine'
import { padEnd, repeat } from 'lodash'
import * as chalk from 'chalk'

const devPrefix = process.env.ENV === 'DEV' ? 'dev.' : ''

export const consoleURL = (token: string, projectName?: string) =>
  `https://${devPrefix}console.graph.cool/token?token=${token}${projectName
    ? `&redirect=/${encodeURIComponent(projectName)}`
    : ''}`
// export const playgroundURL = (token: string, projectName: string) =>
//   `https://console.graph.cool/token?token=${token}&redirect=/${encodeURIComponent(projectName)}/playground`
export const playgroundURL = (projectId: string, localhost?: string) => {
  if (localhost) {
    return `${localhost}/simple/${projectId}`
  }
  return `https://${devPrefix}api.graph.cool/simple/v1/${projectId}`
}

const subscriptionEndpoints = {
  EU_WEST_1: 'wss://subscriptions.graph.cool',
  US_WEST_2: 'wss://subscriptions.us-west-2.graph.cool',
  AP_NORTHEAST_1: 'wss://subscriptions.ap-northeast-1.graph.cool',
}

export const subscriptionURL = (region: Region, projectId: string) =>
  `${subscriptionEndpoints[region]}/v1/${projectId}`

export function sortByTimestamp(a, b) {
  return a.timestamp < b.timestamp ? -1 : 1
}

/**
 * Print a list of [['key', 'value'],...] pairs properly padded
 * @param {string[][]} arr1
 * @param {number} spaceLeft
 * @param {number} spaceBetween
 */
export function printPadded(
  arr1: string[][],
  spaceLeft: number = 0,
  spaceBetween: number = 1,
  header?: string[]
) {
  const inputRows = arr1
  if (header) {
    inputRows.unshift(header)
  }
  const leftCol = inputRows.map(a => a[0])
  const rightCol = inputRows.map(a => a[1])
  const maxLeftCol = leftCol.reduce(
    (acc, curr) => Math.max(acc, curr.length),
    -1,
  )
  const maxRightCol = rightCol.reduce(
    (acc, curr) => Math.max(acc, curr.length),
    -1,
  )
  const paddedLeftCol = leftCol.map(
    v => repeat(' ', spaceLeft) + padEnd(v, maxLeftCol + spaceBetween),
  )

  const rows = paddedLeftCol.map((l, i) => l + arr1[i][1])

  if (header) {
    const divider = `${repeat('─', maxLeftCol)}${repeat(' ', spaceBetween)}${repeat('─', maxRightCol)}`
    rows.splice(1, 0, divider)
  }

  return rows.join('\n')
}

export const prettyProject = p => `${chalk.bold(p.name)} (${p.id})`
