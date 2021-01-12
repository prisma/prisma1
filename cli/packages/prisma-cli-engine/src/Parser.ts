import 'source-map-support/register'
import { Arg, Flags } from './Flags/index'
import { Command } from './Command'
import { RequiredFlagError } from './errors/RequiredFlagError'
import chalk from 'chalk'

export interface ParserSettings {
  flags?: Flags
  args?: Arg[]
  cmd?: Command
  variableArgs?: boolean
}

export interface ParserOutput {
  argv?: string[]
  args?: OutputFlags
  flags?: OutputArgs
}

export interface OutputFlags {
  [name: string]: any
}
export interface OutputArgs {
  [name: string]: string | boolean
}

export class Parser {
  input: ParserSettings

  constructor(input: ParserSettings) {
    this.input = input
    input.args = input.args || []
    input.flags = input.flags || {}
  }

  async parse(output: ParserOutput = {}): Promise<ParserOutput> {
    const argv = output.argv || []
    output.flags = output.flags || {}
    output.argv = []
    output.args = {}

    const parseFlag = arg => {
      const long = arg.startsWith('--')
      const name = long ? findLongFlag(arg) : findShortFlag(arg)

      if (!name) {
        const i = arg.indexOf('=')
        if (i !== -1) {
          const sliced = arg.slice(i + 1)
          argv.unshift(sliced)

          const equalsParsed = parseFlag(arg.slice(0, i))
          if (!equalsParsed) {
            argv.shift()
          }
          return equalsParsed
        } else if (
          this.input.cmd &&
          !(this.input.cmd!.constructor as any).allowAnyFlags
        ) {
          if (this.input.cmd.constructor.name === 'Export' && arg === '-E') {
            throw new Error(
              `-E has been renamed to -e. -e has been renamed to -p. Get more information with ${chalk.bold.green(
                'prisma1 export -h',
              )}`,
            )
          } else {
            throw new Error(`Unknown flag ${arg}`)
          }
        }
        return false
      }
      const flag = this.input.flags![name]
      const cur = output.flags![name]
      if (flag.parse) {
        // TODO: handle multiple flags
        if (cur) {
          throw new Error(`Flag --${name} already provided`)
        }
        const input =
          long || arg.length < 3
            ? argv.shift()
            : arg.slice(arg[2] === '=' ? 3 : 2)
        if (!input) {
          throw new Error(`Flag --${name} expects a value`)
        }
        output.flags![name] = input
      } else {
        if (!cur) {
          output.flags![name] = true
        }
        // push the rest of the short characters back on the stack
        if (!long && arg.length > 2) {
          argv.unshift(`-${arg.slice(2)}`)
        }
      }
      return true
    }

    const findLongFlag = arg => {
      const name = arg.slice(2)
      if (this.input.flags![name]) {
        return name
      }
      return null
    }

    const findShortFlag = arg => {
      for (const k of Object.keys(this.input.flags as any)) {
        if (this.input.flags![k as any].char === arg[1]) {
          return k as any
        }
      }
      return null
    }

    let parsingFlags = true
    const maxArgs = this.input.args!.length
    const minArgs = this.input.args!.filter(a => a.required).length

    while (argv.length) {
      const arg = argv.shift()!
      if (parsingFlags && arg.startsWith('-')) {
        // attempt to parse as flag
        if (arg === '--') {
          parsingFlags = false
          continue
        }
        if (parseFlag(arg)) {
          continue
        }
        // not actually a flag if it reaches here so parse as an arg
      }
      // not a flag, parse as arg
      const argDefinition = this.input.args![output.argv.length]
      if (argDefinition) {
        output.args[argDefinition.name] = arg
      }
      output.argv.push(arg)
    }

    // TODO find better solution
    if (!this.input.variableArgs && output.argv.length > maxArgs + 2) {
      throw new Error(`Unexpected argument ${output.argv[maxArgs]}`)
    }
    if (output.argv.length < minArgs) {
      throw new Error(
        `Missing required argument ${
          this.input.args![output.argv.length].name
        }`,
      )
    }

    for (const name of Object.keys(this.input.flags as any)) {
      const flag = this.input.flags![name]
      if (flag.parse) {
        output.flags[name] = await flag.parse(
          ['string', 'number', 'boolean'].includes(typeof output.flags[name])
            ? String(output.flags[name])
            : undefined,
          this.input.cmd,
          name,
        )
        if (flag.required && !output.flags[name]) {
          throw new RequiredFlagError(name)
        }
      }
    }

    return output
  }
}
