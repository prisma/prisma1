#!/usr/bin/env node

import * as minimist from 'minimist'
import { AuthTrigger, Command, SystemEnvironment } from './types'
import pushCommand from './commands/push'
import consoleCommand from './commands/console'
import playgroundCommand from './commands/playground'
import projectsCommand from './commands/projects'
import pullCommand from './commands/pull'
import authCommand from './commands/auth'
import initCommand from './commands/init'
import interactiveInitCommand from './commands/interactiveInit'
import exportCommand from './commands/export'
import endpointsCommand from './commands/endpoints'
import statusCommand from './commands/status'
import quickstartCommand from './commands/quickstart'
import deleteCommand from './commands/delete'
import FileSystemResolver from './system/FileSystemResolver'
import figures = require('figures')
import StdOut from './system/StdOut'
import { GraphcoolAuthServer } from './api/GraphcoolAuthServer'
import {
  sentryDSN,
  graphcoolConfigFilePath,
  unknownOptionsWarning
} from './utils/constants'
import {
  usageRoot,
} from './utils/usage'
import {optionsForCommand, usageForCommand} from './utils/arguments'

const Raven = require('raven')
const debug = require('debug')('graphcool')
const {version} = require('../../package.json')

import {Config} from './utils/config'

async function main() {
  // initialize sentry
  Raven.config(sentryDSN).install()

  const env = defaultEnvironment()
  const argv = minimist(process.argv.slice(2))

  const command = argv._[0] as Command | undefined
  if (command) {
    checkHelp(command, argv)
    checkOptions(command, argv)
  }

  const displayQuickstart = shouldDisplayQuickstart()

  switch (command) {

    case undefined: {
      if (argv['version'] || argv['v']) {
        process.stdout.write(version)
        break
      }

      process.stdout.write(usageRoot(displayQuickstart))
      process.exit(0)
    }

    // TODO remove legacy support
    case 'create': {
      await checkAuth(env, 'auth')

      console.log('`graphcool create` is deprecated and will be removed in a future version. Use `graphcool init` instead.')

      const name = argv['name'] || argv['n']
      const alias = argv['alias'] || argv['a']
      const region = argv['region'] || argv['r']
      const schemaUrl = argv._[1]
      const remoteSchemaUrl = schemaUrl
      const localSchemaFile = schemaUrl

      const props = {name, alias, remoteSchemaUrl, localSchemaFile, region}
      await initCommand(props, env)
      break
    }

    case 'init': {

      const name = argv['name'] || argv['n']
      const alias = argv['alias'] || argv['a']
      const region = argv['region'] || argv['r']
      const schemaUrl = argv['schema'] || argv['s']
      const copyProjectId = argv['copy'] || argv['c']
      const copyOptions = argv['copy-options']
      const outputPath = argv['output'] || argv['o']

      if (!schemaUrl && !copyProjectId) {
        const props = {name, alias, outputPath, checkAuth}
        await interactiveInitCommand(props, env)
      } else {
        await checkAuth(env, 'init')
        const props = {name, alias, schemaUrl, copyProjectId, copyOptions, region, outputPath}
        await initCommand(props, env)
      }
      break
    }

    case 'push': {
      await checkAuth(env, 'auth')
      checkOptions('push', argv)


      const force = !!(argv['force'] || argv['f'])
      const projectFile = argv._[1]
      await pushCommand({force, projectFile}, env)
      break
    }

    case 'delete': {
      await checkAuth(env, 'auth')

      const sourceProjectId = argv['project'] || argv['p']
      await deleteCommand({sourceProjectId}, env)
      break
    }

    case 'pull': {
      await checkAuth(env, 'auth')

      const sourceProjectId = argv['project'] || argv['p']
      const projectFile = argv._[1]
      const outputPath = argv['output'] || argv['o']
      const force = !!(argv['force'] || argv['f'])
      const props = {sourceProjectId, projectFile, outputPath, force}
      await pullCommand(props, env)
      break
    }

    case 'export': {
      await checkAuth(env, 'auth')

      const projectFile = argv._[1]
      await exportCommand({projectFile}, env)
      break
    }

    case 'status': {
      await checkAuth(env, 'auth')

      const projectFile = argv._[1]
      await statusCommand({projectFile}, env)
      break
    }

    case 'endpoints': {
      await checkAuth(env, 'auth')

      const projectFile = argv._[1]
      await endpointsCommand({projectFile}, env)
      break
    }

    case 'console': {
      await checkAuth(env, 'auth')

      const projectFile = argv._[1]
      await consoleCommand({projectFile}, env)
      break
    }

    case 'playground': {
      await checkAuth(env, 'auth')

      const projectFile = argv._[1]
      await playgroundCommand({projectFile}, env)
      break
    }

    case 'projects': {
      await checkAuth(env, 'auth')

      await projectsCommand({}, env)
      break
    }

    case 'auth': {

      const token = argv['token'] || argv['t']
      await authCommand({token}, env, new GraphcoolAuthServer('auth'))
      break
    }

    case 'quickstart': {

      const props = {checkAuth}
      await quickstartCommand(props, env)

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

    default: {
      if (argv['version'] || argv['v']) {
        process.stdout.write(version)
        break
      }

      process.stdout.write(`Unknown command: ${command}\n\n${usageRoot(displayQuickstart)}`)
      break
    }
  }

  process.stdout.write('\n')
}

async function checkAuth(env: SystemEnvironment, authTrigger: AuthTrigger): Promise<boolean> {
  if (env.config.get('token')) {
    return true
  }

  await authCommand({}, env, new GraphcoolAuthServer(authTrigger))
  return false
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

function checkHelp(command: Command, argv: any) {
  const usage = usageForCommand(command)
  if (argv['help'] || argv['h']) {
    process.stdout.write(usage)
    process.exit(0)
  }
}

function checkOptions(command: Command, inputArgs: any) {
  const allowedOptions = optionsForCommand(command)
  const input = Object.keys(inputArgs)
  const unknownOptions = input.filter(x => allowedOptions.indexOf(x) < 0).filter(x => x !== '_')
  if (unknownOptions.length > 0) {
    const errorMessage = unknownOptionsWarning(command, unknownOptions)
    process.stderr.write(errorMessage)
    process.exit(0)
  }
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
