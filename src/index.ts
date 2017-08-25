#!/usr/bin/env node

import { CommandInstruction } from './types'
import pushCommand, { PushProps } from './commands/push'
import consoleCommand, { ConsoleProps } from './commands/console'
import playgroundCommand, { PlaygroundProps } from './commands/playground'
import projectsCommand from './commands/projects'
import pullCommand, { PullProps } from './commands/pull'
import initCommand, { InitProps } from './commands/init'
import exportCommand, { ExportProps } from './commands/export'
import endpointsCommand, { EndpointsProps } from './commands/endpoints'
import statusCommand, { StatusProps } from './commands/status'
import quickstartCommand from './commands/quickstart'
import deleteCommand, { DeleteProps } from './commands/delete'
import authCommand, { AuthProps } from './commands/auth'
import { parseCommand } from './utils/parseCommand'
import { checkAuth } from './utils/auth'
import { GraphcoolAuthServer } from './io/GraphcoolAuthServer'
import { sentryDSN, } from './utils/constants'
import { usageRoot, } from './utils/usage'
import env from './io/Environment'
import { pick } from 'lodash'

const Raven = require('raven')
const debug = require('debug')('graphcool')
const {version} = require('../../package.json')

async function main() {
  // initialize sentry
  Raven.config(sentryDSN).install()

  const {command, props}: CommandInstruction = await parseCommand(process.argv, version)

  switch (command) {

    case undefined: {
      process.stdout.write(usageRoot())
      process.exit(0)
    }
    case 'init': {
      await checkAuth('init')
      await initCommand(props as InitProps)
      break
    }

    // case 'interactiveInit': {
    //   await interactiveInitCommand(props as InteractiveInitProps)
    //   break
    // }

    case 'push': {
      await checkAuth('auth')
      await pushCommand(props as PushProps)
      break
    }

    case 'delete': {
      await checkAuth('auth')
      const projectId = env.getProjectId(props)
      await deleteCommand({projectId})
      break
    }

    case 'pull': {
      await checkAuth('auth')
      await pullCommand(props as PullProps)
      break
    }

    case 'export': {
      await checkAuth('auth')
      await exportCommand(props as ExportProps)
      break
    }

    case 'status': {
      await checkAuth('auth')
      await statusCommand(props as StatusProps)
      break
    }

    case 'endpoints': {
      await checkAuth('auth')
      await endpointsCommand(props as EndpointsProps)
      break
    }

    case 'console': {
      await checkAuth('auth')
      await consoleCommand(props as ConsoleProps)
      break
    }

    case 'playground': {
      await checkAuth('auth')
      await playgroundCommand(props as PlaygroundProps)
      break
    }

    case 'projects': {
      await checkAuth('auth')
      await projectsCommand({})
      break
    }

    case 'auth': {
      await authCommand(props as AuthProps, new GraphcoolAuthServer('auth'))
      break
    }

    case 'quickstart': {
      await quickstartCommand({})
      break
    }

    case 'help': {
      process.stdout.write(usageRoot())
      process.exit(0)
      break
    }

    case 'version': {
      process.stdout.write(version)
      break
    }

    case 'unknown':
    default: {
      process.stdout.write(`Unknown command: ${command}\n\n${usageRoot()}`)
      break
    }
  }

  process.stdout.write('\n')
}

process.on('unhandledRejection', e => new StdOut().onError(e))

main()
