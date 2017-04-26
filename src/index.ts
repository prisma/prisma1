#!/usr/bin/env node

import * as minimist from 'minimist'
import {Command} from './types'
import pushCommand from './commands/push'
import projectsCommand from './commands/projects'
import pullCommand from './commands/pull'
import FileSystemResolver from './resolvers/FileSystemResolver'
const debug = require('debug')('graphcool')
import figures = require('figures')
import * as chalk from 'chalk'
import {usagePull, usageProjects, usageCreate, usageRoot} from './utils/usage'

async function main() {
  const argv = minimist(process.argv.slice(2))

  const command = argv._[0] as Command | undefined

  process.stdout.write('\n')

  switch (command) {

    case undefined: {
      process.stdout.write(usageRoot)
      process.exit(0)
    }

    case 'push': {
      checkHelp(argv, usageCreate)

      const isDryRun = true // (argv['dry'] || argv['d']) ? true : false
      const projectFilePath = argv['path'] || argv['p']
      const resolver = new FileSystemResolver()
      await pushCommand({isDryRun, projectFilePath}, resolver)
      break
    }

    case 'projects': {
      checkHelp(argv, usageProjects)

      await projectsCommand({}, new FileSystemResolver())
      break
    }

    case 'pull': {
      checkHelp(argv, usagePull)

      const projectId = argv['project-id'] || argv['p']
      await pullCommand({projectId}, new FileSystemResolver())
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

function checkHelp(argv: any, usage: string) {
  if (argv['help'] || argv['h']) {
    process.stdout.write(usage)
    process.exit(0)
  }
}

function onError(e: Error) {
  console.log(`${chalk.red(figures.cross)} Error: ${e.message}\n`)
  console.error(e.stack)
  process.exit(1)
}

process.on('unhandledRejection', e => onError(e))
main()


