#!/usr/bin/env node

import { AuthTrigger, Command, CommandInstruction} from './types'
import pushCommand, { PushProps } from './commands/push'
import consoleCommand, { ConsoleProps } from './commands/console'
import playgroundCommand, { PlaygroundProps } from './commands/playground'
import projectsCommand from './commands/projects'
import pullCommand, { PullProps } from './commands/pull'
// import initCommand, { InitProps } from './commands/init'
// import interactiveInitCommand, { InteractiveInitProps } from './commands/interactiveInit'
import exportCommand, { ExportProps } from './commands/export'
import endpointsCommand, { EndpointsProps } from './commands/endpoints'
import statusCommand, { StatusProps } from './commands/status'
import quickstartCommand from './commands/quickstart'
import deleteCommand, { DeleteProps } from './commands/delete'
import authCommand, { AuthProps } from './commands/auth'
import { parseCommand } from './utils/parseCommand'
import { checkAuth } from './utils/auth'
import FileSystemResolver from './system/FileSystemResolver'
import { GraphcoolAuthServer } from './io/GraphcoolAuthServer'
import {
  sentryDSN,
  graphcoolConfigFilePath,
} from './utils/constants'
import {
  usageRoot,
} from './utils/usage'

const Raven = require('raven')
const debug = require('debug')('graphcool')
const {version} = require('../../package.json')

async function main() {
  // initialize sentry
  Raven.config(sentryDSN).install()

  const displayQuickstart = shouldDisplayQuickstart()

  const {command, props}: CommandInstruction = await parseCommand(process.argv, version)

  switch (command) {

    case undefined: {
      process.stdout.write(usageRoot())
      process.exit(0)
    }
    // TODO reenable later when we have the new init flow defined
    //
    // case 'init': {
    //   await checkAuth('init')
    //   await initCommand(props as InitProps)
    //   break
    // }
    //
    // case 'interactiveInit': {
    //   await interactiveInitCommand(props as InteractiveInitProps, env)
    //   break
    // }

    case 'push': {
      await checkAuth('auth')
      await pushCommand(props as PushProps)
      break
    }

    case 'delete': {
      await checkAuth('auth')
      await deleteCommand(props as DeleteProps, env)
      break
    }

    case 'pull': {
      await checkAuth('auth')
      await pullCommand(props as PullProps, env)
      break
    }

    case 'export': {
      await checkAuth('auth')
      await exportCommand(props as ExportProps, env)
      break
    }

    case 'status': {
      await checkAuth('auth')
      await statusCommand(props as StatusProps, env)
      break
    }

    case 'endpoints': {
      await checkAuth('auth')
      await endpointsCommand(props as EndpointsProps, env)
      break
    }

    case 'console': {
      await checkAuth('auth')
      await consoleCommand(props as ConsoleProps, env)
      break
    }

    case 'playground': {
      await checkAuth('auth')
      await playgroundCommand(props as PlaygroundProps, env)
      break
    }

    case 'projects': {
      await checkAuth('auth')
      await projectsCommand({}, env)
      break
    }

    case 'auth': {
      await authCommand(props as AuthProps, new GraphcoolAuthServer('auth'))
      break
    }

    case 'quickstart': {
      await quickstartCommand({}, env)
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
