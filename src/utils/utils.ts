import {ProjectInfo} from '../types'
import * as chalk from 'chalk'
import {setDebugMessage, contactUsInSlackMessage} from './constants'
import figures = require('figures')

var Raven = require('raven')
const debug = require('debug')('graphcool')

export function projectInfoToContents(projectInfo: ProjectInfo): string {
  return `# project: ${projectInfo.alias || projectInfo.projectId}\n# version: ${projectInfo.version}\n\n${projectInfo.schema}`
}

// export function onError(e: Error, reportToSentry: boolean = true) {
//   if (reportToSentry) {
//     Raven.captureException(e)
//   }
//
//   // prevent the same error output twice
//   const errorMessage = `Error: ${e.message}`
//   if (e.stack && !e.stack.startsWith(errorMessage!)) {
//     console.error(`${chalk.red(figures.cross)}  Error: ${errorMessage}\n`)
//     debug(e.stack)
//   } else {
//     const errorLines = e.stack!.split('\n')
//     const firstErrorLine = errorLines[0]
//     console.error(`${chalk.red(figures.cross)}  ${firstErrorLine}`)
//     debug(e.stack)
//   }
//
//   console.error(`\n${setDebugMessage}\n${contactUsInSlackMessage}`)
//   process.exit(1)
// }
