#!/usr/bin/env node

import * as minimist from 'minimist'
import {Command, SystemEnvironment} from './types'
import pushCommand from './commands/push'
import projectsCommand from './commands/projects'
import pullCommand from './commands/pull'
import authCommand from './commands/auth'
import initCommand from './commands/init'
import FileSystemResolver from './system/FileSystemResolver'
const debug = require('debug')('graphcool')
import figures = require('figures')
import * as chalk from 'chalk'
import {usagePull, usageProjects, usageInit, usageRoot, usagePush, usageAuth} from './utils/usage'
import StdOut from './system/StdOut'
import {GraphcoolAuthServer} from './api/GraphcoolAuthServer'
import {readGraphcoolConfig} from './utils/file'

async function main() {
  const argv = minimist(process.argv.slice(2))

  const command = argv._[0] as Command | undefined

  process.stdout.write('\n')

  switch (command) {

    case undefined: {
      process.stdout.write(usageRoot)
      process.exit(0)
    }

    case 'init': {
      checkHelp(argv, usageInit)
      await checkAuth()

      const name = argv['name'] || argv['n']
      const alias = argv['alias'] || argv['a']
      const region = argv['region'] || argv['r']
      const remoteSchemaUrl = argv['url'] || argv['u']
      const localSchemaFile =  argv['file'] || argv['f']

      const props = {name, alias, remoteSchemaUrl, localSchemaFile, region}
      await initCommand(props, defaultEnvironment())
      break
    }

    case 'push': {
      checkHelp(argv, usagePush)
      await checkAuth()

      const isDryRun = true // (argv['dry'] || argv['d']) ? true : false
      const projectFilePath = argv['path'] || argv['p']
      await pushCommand({isDryRun, projectFilePath}, defaultEnvironment())
      break
    }

    case 'projects': {
      checkHelp(argv, usageProjects)
      await checkAuth()

      await projectsCommand({}, defaultEnvironment())
      break
    }

    case 'pull': {
      checkHelp(argv, usagePull)
      await checkAuth()

      const projectId = argv['project-id'] || argv['p']
      await pullCommand({projectId}, defaultEnvironment())
      break
    }

    case 'auth': {
      checkHelp(argv, usageAuth)

      const token = argv['token'] || argv['t']
      await authCommand({token}, defaultEnvironment(), new GraphcoolAuthServer())
      break
    }

    case 'help': {
      process.stdout.write(usageRoot)
      process.exit(0)
      break
    }

    default: {
      process.stdout.write(`Unknown command: ${command}\n\n${usageRoot}`)
      break
    }
  }

  process.stdout.write('\n\n')
}

async function checkAuth() {
  try {
    readGraphcoolConfig(new FileSystemResolver())
  } catch (e) {
    await authCommand({}, defaultEnvironment(), new GraphcoolAuthServer())
  }
}

function checkHelp(argv: any, usage: string) {
  if (argv['help'] || argv['h']) {
    process.stdout.write(usage)
    process.exit(0)
  }
}

function defaultEnvironment(): SystemEnvironment {
  return {
    resolver: new FileSystemResolver(),
    out: new StdOut()
  }
}

function onError(e: Error) {
  console.log(`${chalk.red(figures.cross)} Error: ${e.message}\n`)
  console.error(e.stack)
  process.exit(1)
}

process.on('unhandledRejection', e => onError(e))
main()


