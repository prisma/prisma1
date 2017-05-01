#!/usr/bin/env node

import * as minimist from 'minimist'
import { Command, SystemEnvironment } from './types'
import pushCommand from './commands/push'
import consoleCommand from './commands/console'
import projectsCommand from './commands/projects'
import pullCommand from './commands/pull'
import authCommand from './commands/auth'
import initCommand from './commands/init'
import interactiveInitCommand from './commands/interactiveInit'
import exportCommand from './commands/export'
import FileSystemResolver from './system/FileSystemResolver'
import figures = require('figures')
import * as chalk from 'chalk'
import StdOut from './system/StdOut'
import { GraphcoolAuthServer } from './api/GraphcoolAuthServer'
import { readGraphcoolConfig } from './utils/file'
import { graphcoolProjectFileName, setDebugMessage } from './utils/constants'
import {
  usagePull,
  usageProjects,
  usageInit,
  usageRoot,
  usagePush,
  usageAuth,
  usageVersion,
  usageConsole,
  usageExport,
} from './utils/usage'

const debug = require('debug')('graphcool')
const {version} = require('../../package.json')

async function main() {
  const argv = minimist(process.argv.slice(2))

  const command = argv._[0] as Command | undefined

  switch (command) {

    case undefined: {
      process.stdout.write(usageRoot)
      process.exit(0)
    }

    // TODO remove legacy support
    case 'create': {
      checkHelp(argv, usageInit)
      await checkAuth()

      const name = argv['name'] || argv['n']
      const alias = argv['alias'] || argv['a']
      const region = argv['region'] || argv['r']
      const schemaUrl = argv._[1]
      const remoteSchemaUrl = schemaUrl
      const localSchemaFile = schemaUrl

      const props = {name, alias, remoteSchemaUrl, localSchemaFile, region}
      await initCommand(props, defaultEnvironment())
      break
    }

    case 'init': {
      checkHelp(argv, usageInit)

      const name = argv['name'] || argv['n']
      const alias = argv['alias'] || argv['a']
      const region = argv['region'] || argv['r']
      const remoteSchemaUrl = argv['url'] || argv['u']
      const localSchemaFile = argv['file'] || argv['f']
      const outputPath = argv['output'] || argv['o']

      if (!remoteSchemaUrl && !localSchemaFile) {
        await interactiveInitCommand({checkAuth}, defaultEnvironment())
      } else {
        await checkAuth()

        const props = {name, alias, remoteSchemaUrl, localSchemaFile, region, outputPath}
        await initCommand(props, defaultEnvironment())
      }
      break
    }

    case 'push': {
      checkHelp(argv, usagePush)
      await checkAuth()

      const isDryRun = !!(argv['dry-run'] || argv['d'])
      const projectFilePath = (argv['project'] || argv['p'])
      await pushCommand({isDryRun, projectFilePath}, defaultEnvironment())
      break
    }

    case 'pull': {
      checkHelp(argv, usagePull)
      await checkAuth()

      const sourceProjectId = argv['source'] || argv['s']
      const projectFile = argv['project'] || argv['p']
      const outputPath = argv['output'] || argv['o']
      await pullCommand({sourceProjectId, projectFile, outputPath}, defaultEnvironment())
      break
    }

    case 'export': {
      checkHelp(argv, usageExport)
      await checkAuth()

      const sourceProjectId = argv['source'] || argv['s']
      const projectFile = argv['project'] || argv['p']
      await exportCommand({sourceProjectId, projectFile}, defaultEnvironment())
      break
    }

    case 'console': {
      checkHelp(argv, usageConsole)
      await checkAuth()

      await consoleCommand({}, defaultEnvironment())
      break
    }

    case 'projects': {
      checkHelp(argv, usageProjects)
      await checkAuth()

      await projectsCommand({}, defaultEnvironment())
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

    case 'version': {
      checkHelp(argv, usageVersion)

      process.stdout.write(version)
      break
    }

    default: {
      process.stdout.write(`Unknown command: ${command}\n\n${usageRoot}`)
      break
    }
  }

  process.stdout.write('\n')
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
  console.error(`${chalk.red(figures.cross)}  Error: ${e.message}\n`)
  console.error(e.stack)
  console.error(`\n${setDebugMessage}`)
  process.exit(1)
}

process.on('unhandledRejection', e => onError(e))

main()
