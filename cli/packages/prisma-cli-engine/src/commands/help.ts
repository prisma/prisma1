import 'source-map-support/register'
import { stdtermwidth } from '../Output/actions/screen'
import { compare, linewrap } from '../util'
import { Command } from '../Command'
import Plugins from '../Plugin/Plugins'
import chalk from 'chalk'
import { groupBy, flatten, padStart } from 'lodash'
const debug = require('debug')('help command')

function trimToMaxLeft(n: number): number {
  const max = Math.floor(stdtermwidth * 0.8)
  return n > max ? max : n
}

function maxLength(items: string[]) {
  return items.reduce((acc, curr) => Math.max(acc, curr.length), -1)
}

export function renderList(
  items: string[][],
  globalMaxLeftLength?: number,
): string {
  const maxLeftLength =
    globalMaxLeftLength || maxLength(items.map(i => i[0])) + 2
  return items
    .map(i => {
      let left = `  ${i[0]}`
      let right = i[1]
      if (!right) {
        return left
      }
      left = `${padStart(left, maxLeftLength)}`
      right = linewrap(maxLeftLength + 2, right)
      return `${left}  ${right}`
    })
    .join('\n')
}

export default class Help extends Command {
  static topic = 'help'
  static description = 'display help'
  static variableArgs = true
  static allowAnyFlags = true

  plugins: Plugins

  async run() {
    this.plugins = new Plugins(this.out)
    await this.plugins.load()
    const commandFinder = arg => !['help', '-h', '--help'].includes(arg)
    const argv = this.config.argv.slice(1)
    const firstCommandIndex = argv.findIndex(commandFinder)

    let cmd = argv[firstCommandIndex]
    if (argv.length > firstCommandIndex + 1) {
      const secondCommand = argv
        .slice(1)
        .slice(firstCommandIndex)
        .find(commandFinder)
      if (secondCommand) {
        cmd = `${argv[firstCommandIndex]}:${secondCommand}`
      }
    }

    debug(`argv`, argv)
    debug(`cmd`, cmd)
    if (!cmd) {
      return this.topics()
    }

    const topic = await this.plugins.findTopic(cmd)
    const matchedCommand = await this.plugins.findCommand(cmd)

    if (!topic && !matchedCommand) {
      throw new Error(`command ${cmd} not found`)
    }

    if (matchedCommand) {
      this.out.log(matchedCommand.buildHelp(this.config))
    }

    if (topic) {
      const cmds = await this.plugins.commandsForTopic(topic.id)
      const subtopics = await this.plugins.subtopicsForTopic(topic.id)
      if (subtopics && subtopics.length) {
        await this.topics(subtopics, topic.id, topic.id.split(':').length + 1)
      }
      if (cmds && cmds.length > 0 && cmds[0].command) {
        this.listCommandsHelp(cmd, cmds)
      }
    }
  }

  async topics(
    ptopics: any[] | null = null,
    id: string | null = null,
    offset: number = 1,
  ) {
    const color = this.out.color
    this.out.log(`\nPrisma replaces traditional ORMs (${chalk.underline(
      'https://www.prisma.io',
    )})
    
${chalk.bold('Usage:')} ${chalk.bold('prisma')} COMMAND`)
    const topics = (ptopics || this.plugins.topics).filter(t => {
      if (!t.id) {
        return
      }
      const subtopic = t.id.split(':')[offset]
      return !t.hidden && !subtopic
    })
    const groupedTopics = groupBy(topics, topic => topic.group)
    const jobs: any[] = []
    for (const group of this.plugins.groups) {
      // debugger
      const groupTopics = groupedTopics[group.key]
      // const list = groupTopics.map(t => [
      //   t.id,
      //   t.description ? chalk.dim(t.description) : null,
      // ])
      if (!groupTopics) {
        continue
      }
      const list: string[][][] = (await Promise.all(
        groupTopics.map(async (t: any) => {
          const cmds = await this.plugins.commandsForTopic(t.id)
          // console.log(cmds)
          // if (t.id === 'cluster') {
          //   debugger
          // }
          return cmds
            .filter(cmd => !cmd.hidden)
            .map(cmd => {
              const cmdName = cmd.command ? ` ${cmd.command}` : ''
              const deprecation = cmd.deprecated ? ' (deprecated)' : ''
              return [
                t.id + cmdName,
                chalk.dim((cmd.description || t.description) + deprecation),
              ]
            })
        }),
      )) as any
      const name = group.deprecated ? `${group.name} (deprecated)` : group.name
      jobs.push({
        deprecated: group.deprecated,
        group: name,
        list: flatten(list),
      })
    }

    const globalMaxLeft =
      maxLength(flatten(jobs.map(j => j.list)).map(i => i[0])) + 6

    jobs.forEach(job => {
      this.out.log('')
      const firstLine = chalk.bold(job.group + ':')
      const secondLine = renderList(job.list, globalMaxLeft)
      this.out.log(job.deprecated ? chalk.dim(firstLine) : firstLine)
      this.out.log(job.deprecated ? chalk.dim(secondLine) : secondLine)
    })

    this.out.log(`\nUse ${chalk.cyan(
      'prisma help [command]',
    )} for more information about a command.
Docs can be found here: https://bit.ly/prisma-cli-commands

${chalk.dim('Examples:')}

${chalk.gray('-')} Initialize files for a new Prisma service
  ${chalk.cyan('$ prisma init')}

${chalk.gray('-')} Deploy service changes (or new service)
  ${chalk.cyan('$ prisma deploy')}
`)
  }

  listCommandsHelp(topic: string, commands: Array<typeof Command>) {
    commands = commands.filter(c => !c.hidden)
    if (commands.length === 0) {
      return
    }
    commands.sort(compare('command'))
    const helpCmd = this.out.color.cmd(
      `${this.config.bin} help ${topic} COMMAND`,
    )
    this.out.log(
      `${this.out.color.bold(this.config.bin)} ${this.out.color.bold(
        topic,
      )} commands: (get help with ${helpCmd})\n`,
    )
    this.out.log(renderList(commands.map(c => c.buildHelpLine(this.config))))
    if (commands.length === 1 && (commands[0] as any).help) {
      this.out.log((commands[0] as any).help)
    } else {
      this.out.log('')
    }
  }
}
