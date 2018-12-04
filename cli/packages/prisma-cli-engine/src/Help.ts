import { stdtermwidth } from './Output/actions/screen'
import { Arg } from './Flags/index'
import { Output } from './Output/index'
import { Config } from './Config'
import { linewrap } from './util'
import chalk from 'chalk'
import { padStart, maxBy } from 'lodash'

function renderList(items: string[][]): string {
  const maxLength = maxBy(items, '[0].length')![0].length
  const lines = items.map(i => {
    let left = i[0]
    let right = i[1]
    if (!right) {
      return left
    }
    left = `${padStart(left, maxLength)}`
    right = linewrap(maxLength + 2, right)
    return `${left}    ${right}`
  })
  return lines.join('\n')
}

// TODO find proper typing for this flow definition: Class<Command<*>>
function buildUsage(command: any): string {
  if (command.usage) {
    return command.usage.trim()
  }
  const cmd = command.id.replace(/:/g, ' ')
  if (!command.args) {
    return chalk.bold(cmd.trim()) as any
  }
  const args = command.args.map(renderArg)
  return `${chalk.bold(cmd)} ${args.join(' ')}`.trim()
}

function renderArg(arg: Arg): string {
  const name = arg.name.toUpperCase()
  if (arg.required) {
    return `${name}`
  } else {
    return `[${name}]`
  }
}

export default class Help {
  config: Config
  out: Output

  constructor(config: Config, output?: Output) {
    this.config = config
    this.out = output || new Output(config)
  }

  // TODO: command (cmd: Class<Command<*>>): string {
  command(cmd: any): string {
    const color = this.out.color
    const flags = Object.keys(cmd.flags || {})
      .map(f => [f, cmd.flags[f]])
      .filter(f => !f[1].hidden)
    const args = (cmd.args || []).filter(a => !a.hidden)
    const hasFlags = flags.length ? ` ${color.green('[flags]')}` : ''
    const usage = `${color.bold('Usage:')} ${this.config.bin} ${buildUsage(
      cmd,
    )}${hasFlags}\n`
    return [
      usage,
      cmd.description ? `\n${color.bold(cmd.description.trim())}\n` : '',
      this.renderAliases(cmd.aliases),
      this.renderArgs(args),
      this.renderFlags(flags),
      cmd.help ? `\n${cmd.help.trim()}\n` : '',
    ].join('')
  }

  // TODO: commandLine (cmd: Class<Command<*>>): [string, ?string] {
  commandLine(cmd: any): string[] {
    return [
      buildUsage(cmd),
      cmd.description ? this.out.color.dim(cmd.description) : null,
    ]
  }

  renderAliases(aliases?: string[]): string {
    if (!aliases || !aliases.length) {
      return ''
    }
    const a = aliases.map(a => `  $ ${this.config.bin} ${a}`).join('\n')
    return `\n${this.out.color.green('Aliases:')}\n${a}\n`
  }

  renderArgs(args: Arg[]): string {
    if (!args.find(f => !!f.description)) {
      return ''
    }
    return (
      '\n' +
      renderList(
        args.map(a => {
          return [
            a.name.toUpperCase(),
            a.description ? this.out.color.dim(a.description) : null,
          ]
        }),
      ) +
      '\n'
    )
  }

  // TODO renderFlags (flags: [string, Flag][]): string {
  renderFlags(flags: any): string {
    if (!flags.length) {
      return ''
    }
    flags.sort((a, b) => {
      if (a[1].char && !b[1].char) {
        return -1
      }
      if (b[1].char && !a[1].char) {
        return 1
      }
      if (a[0] < b[0]) {
        return -1
      }
      return b[0] < a[0] ? 1 : 0
    })
    return (
      `\n${this.out.color.green('Flags:')}\n` +
      renderList(
        flags.map(([name, f]) => {
          const label: string[] = []
          if (f.char) {
            label.push(`-${f.char}`)
          }
          if (name) {
            label.push(` --${name}`)
          }
          const usage = f.parse ? ` ${name.toUpperCase()}` : ''
          let description = f.description || ''
          if (f.required || f.optional === false) {
            description = `(required) ${description}`
          }
          return [
            ` ${label.join(',').trim()}` + usage,
            description ? this.out.color.dim(description) : null,
          ]
        }),
      ) +
      '\n'
    )
  }
}
