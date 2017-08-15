#!/usr/bin/env node

import { AuthTrigger, Command, CommandInstruction, SystemEnvironment } from './types'
import pushCommand, { PushProps } from './commands/push'
import consoleCommand, { ConsoleProps } from './commands/console'
import playgroundCommand, { PlaygroundProps } from './commands/playground'
import projectsCommand from './commands/projects'
import pullCommand, { PullProps } from './commands/pull'
import initCommand, { InitProps } from './commands/init'
import interactiveInitCommand, { InteractiveInitProps } from './commands/interactiveInit'
import exportCommand, { ExportProps } from './commands/export'
import endpointsCommand, { EndpointsProps } from './commands/endpoints'
import statusCommand, { StatusProps } from './commands/status'
import quickstartCommand from './commands/quickstart'
import deleteCommand, { DeleteProps } from './commands/delete'
import authCommand, { AuthProps } from './commands/auth'
import { parseCommand } from './utils/parseCommand'
import { checkAuth } from './utils/auth'
import FileSystemResolver from './system/FileSystemResolver'
import StdOut from './system/StdOut'
import { GraphcoolAuthServer } from './api/GraphcoolAuthServer'
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

import {Config} from './utils/config'

async function main() {
  // initialize sentry
  Raven.config(sentryDSN).install()

  const env = defaultEnvironment()

  const displayQuickstart = shouldDisplayQuickstart()

  const {command, props}: CommandInstruction = await parseCommand(process.argv, version, env)

  switch (command) {

    case undefined: {
      process.stdout.write(usageRoot(displayQuickstart))
      process.exit(0)
    }

    case 'init': {
      await checkAuth(env, 'init')
      await initCommand(props as InitProps, env)
      break
    }

    case 'interactiveInit': {
      await interactiveInitCommand(props as InteractiveInitProps, env)
      break
    }

    case 'push': {
      await checkAuth(env, 'auth')
      await pushCommand(props as PushProps, env)
      break
    }

    case 'delete': {
      await checkAuth(env, 'auth')
      await deleteCommand(props as DeleteProps, env)
      break
    }

    case 'pull': {
      await checkAuth(env, 'auth')
      await pullCommand(props as PullProps, env)
      break
    }

    case 'export': {
      await checkAuth(env, 'auth')
      await exportCommand(props as ExportProps, env)
      break
    }

    case 'status': {
      await checkAuth(env, 'auth')
      await statusCommand(props as StatusProps, env)
      break
    }

    case 'endpoints': {
      await checkAuth(env, 'auth')
      await endpointsCommand(props as EndpointsProps, env)
      break
    }

    case 'console': {
      await checkAuth(env, 'auth')
      await consoleCommand(props as ConsoleProps, env)
      break
    }

    case 'playground': {
      await checkAuth(env, 'auth')
      await playgroundCommand(props as PlaygroundProps, env)
      break
    }

    case 'projects': {
      await checkAuth(env, 'auth')
      await projectsCommand({}, env)
      break
    }

    case 'auth': {
      await authCommand(props as AuthProps, env, new GraphcoolAuthServer('auth'))
      break
    }

    case 'quickstart': {
      await quickstartCommand({}, env)
      break
    }

    case 'help': {
      process.stdout.write(usageRoot(displayQuickstart))
      process.exit(0)
      break
    }

    case 'version': {
      process.stdout.write(version)
      break
    }

    case 'unknown':
    default: {
      process.stdout.write(`Unknown command: ${command}\n\n${usageRoot(displayQuickstart)}`)
      break
    }
  }

  process.stdout.write('\n')
}

function shouldDisplayQuickstart(): boolean {
  const resolver = new FileSystemResolver()
  if (resolver.exists(graphcoolConfigFilePath)) {
    const content = JSON.parse(resolver.read(graphcoolConfigFilePath))
    if (content.token && content.token.length > 0) {
      return false
    }
  }
  return true
}

function defaultEnvironment(): SystemEnvironment {
  const resolver = new FileSystemResolver()

  var config = new Config(resolver)
  config.load()

  return {
    resolver: resolver,
    out: new StdOut(),
    config: config
  }
}

process.on('unhandledRejection', e => new StdOut().onError(e))

main()
