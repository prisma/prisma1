import { Region } from 'graphcool-cli-engine'
import chalk from 'chalk'

const devPrefix = process.env.ENV === 'DEV' ? 'dev.' : ''

export const consoleURL = (token: string, projectName?: string) =>
  `https://${devPrefix}console.graph.cool/token?token=${token}${projectName
    ? `&redirect=/${encodeURIComponent(projectName)}`
    : ''}`
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
