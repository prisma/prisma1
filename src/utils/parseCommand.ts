import * as minimist from 'minimist'
import { Command, CommandInstruction, SystemEnvironment } from '../types'
import { unknownOptionsWarning } from './constants'
import { optionsForCommand } from './arguments'
import client from '../io/client'

export async function parseCommand(args: string[], version: string): Promise<CommandInstruction> {
  const instruction = getInstruction(args)
  await client.checkStatus(instruction)
  return instruction
}

function getInstruction(args: string[]): CommandInstruction {
  const argv = minimist(args.slice(2))
  const command = argv._[0] as Command | undefined

  if (command) {
    if (argv['help'] || argv['h']) {
      return {
        command: 'usage',
        props: {command}
      }
    }

    if (argv['version'] || argv['h']) {
      return {
        command: 'version'
      }
    }
  } else {
    // will trigger usage
    return {}
  }

  checkOptions(command, argv)

  switch (command) {
    case 'init': {
      const name = argv['name'] || argv['n']
      const alias = argv['alias'] || argv['a']
      const region = argv['region'] || argv['r']
      // if there are exactly 2 argv's, use the 2nd argument as an alternative for the schemaUrl
      const schemaUrl = argv['schema'] || argv['s'] || argv.length === 2 && argv._[1]
      const copyProjectId = argv['copy'] || argv['c']
      const copyOptions = argv['copy-options']
      const outputPath = argv['output'] || argv['o']

      if (!schemaUrl && !copyProjectId) {
        return {
          command: 'interactiveInit',
          props: {name, alias, outputPath, region}
        }
      } else {
        return {
          command: 'init',
          props: {name, alias, schemaUrl, copyProjectId, copyOptions, region, outputPath}
        }
      }
    }

    case 'push': {
      const force = !!(argv['force'] || argv['f'])
      const projectFile = argv._[1]

      return {
        command,
        props: {force, projectFile}
      }
    }

    case 'delete': {
      const project = argv['project'] || argv['p']
      const env = argv['env'] || argv['e']

      return {
        command,
        props: {project, env}
      }
    }

    case 'pull': {
      const project = argv['project'] || argv['p']
      const env = argv['env'] || argv['e']
      const projectFile = argv._[1]
      const outputPath = argv['output'] || argv['o']
      const force = !!(argv['force'] || argv['f'])

      return {
        command,
        props: {project, env, projectFile, outputPath, force},
      }
    }

    case 'export':
    case 'console':
    case 'playground': {
      const env = argv['env'] || argv['e']

      return {
        command,
        props: {env}
      }
    }

    case 'projects': {
      return {
        command,
      }
    }

    case 'auth': {
      const token = argv['token'] || argv['t']

      return {
        command,
        props: {token}
      }
    }

    case 'quickstart':
    case 'help':
    case 'version': {
      return {
        command,
      }
    }

    default: {
      return {
        command: 'unknown'
      }
    }
  }
}

function checkOptions(command: Command, inputArgs: any): void {
  const allowedOptions = optionsForCommand(command)
  const input = Object.keys(inputArgs)
  const unknownOptions = input.filter(x => allowedOptions.indexOf(x) < 0).filter(x => x !== '_')
  if (unknownOptions.length > 0) {
    const errorMessage = unknownOptionsWarning(command, unknownOptions)
    process.stderr.write(errorMessage)
    process.exit(0)
  }
}
