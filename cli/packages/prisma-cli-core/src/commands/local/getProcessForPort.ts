/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

'use strict'

const chalk = require('chalk')
const execSync = require('child_process').execSync
const path = require('path')

const execOptions = {
  encoding: 'utf8',
  stdio: [
    'pipe', // stdin (default)
    'pipe', // stdout (default)
    'ignore', //stderr
  ],
}

function getProcessIdOnPort(port) {
  return execSync('lsof -i:' + port + ' -P -t -sTCP:LISTEN', execOptions)
    .split('\n')[0]
    .trim()
}

function getProcessCommand(processId, processDirectory) {
  let command = execSync(
    'ps -o command -p ' + processId + ' | sed -n 2p',
    execOptions,
  )

  if (command.includes('docker')) {
    return 'docker'
  }

  command = command.replace(/\n$/, '')
}

function getDirectoryOfProcessById(processId) {
  return execSync(
    'lsof -p ' + processId + ' | awk \'$4=="cwd" {print $9}\'',
    execOptions,
  ).trim()
}

export interface ProcessInfo {
  command?: string
  processId: string
}

export function getProcessForPort(port): ProcessInfo | null {
  try {
    const processId = getProcessIdOnPort(port)
    const directory = getDirectoryOfProcessById(processId)
    const command = getProcessCommand(processId, directory)
    return {
      command,
      processId,
    }
  } catch (e) {
    // console.error(e)
    return null
  }
}

export const printProcess = ({ command, processId }: ProcessInfo) =>
  chalk.cyan(command) + chalk.grey(' (pid ' + processId + ')')
