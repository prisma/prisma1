import 'source-map-support/register'
import { stdtermwidth } from '../Output/actions/screen'
import { compare, linewrap } from '../util'
import { Command } from '../Command'
import Plugins from '../Plugin/Plugins'
import * as chalk from 'chalk'

function trimToMaxLeft(n: number): number {
  const max = Math.floor(stdtermwidth * 0.6)
  return n > max ? max : n
}

function trimCmd(s: string, max: number): string {
  if (s.length <= max) {
    return s
  }
  return `${s.slice(0, max - 1)}\u2026`
}

function renderList(items: string[][]): string {
  const S = require('string')
  const max = require('lodash.maxby')

  const maxLeftLength = trimToMaxLeft(max(items, '[0].length')[0].length + 1)
  return items
    .map(i => {
      let left = ` ${i[0]}`
      let right = i[1]
      if (!right) {
        return left
      }
      left = `${S(trimCmd(left, maxLeftLength)).padRight(maxLeftLength)}`
      right = linewrap(maxLeftLength + 2, right)
      return `${left}    ${right}`
    }).join('\n')
}

export default class Help extends Command {
  static topic = 'help'
  static description = 'display help'
  static variableArgs = true

  plugins: Plugins

  async run() {
    this.plugins = new Plugins(this.out)
    await this.plugins.load()
    const cmd = this.config.argv.slice(1).find(arg => !['help', '-h', '--help'].includes(arg))
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
        this.topics(subtopics, topic.id, (topic.id.split(':').length + 1))
      }
      if (cmds) {
        this.listCommandsHelp(cmd, cmds)
      }
    }
  }

  topics(ptopics: any[] | null = null, id: string | null = null, offset: number = 1) {
    const color = this.out.color
    this.out.log(`\nServerless GraphQL backend for frontend developers (${chalk.underline('https://www.graph.cool')})
    
${chalk.bold('Usage:')} ${chalk.bold('graphcool')} COMMAND

${chalk.bold('Commands:')}`)
    let topics = (ptopics || this.plugins.topics).filter(t => {
      if (!t.id) {
        return
      }
      const subtopic = t.id.split(':')[offset]
      return !t.hidden && !subtopic
    })
    topics = topics.map(t => (
      [
        t.id,
        t.description ? chalk.dim(t.description) : null
      ]
    ))
    topics.sort()
    this.out.log(renderList(topics))
    this.out.log(`\nRun ${chalk.bold.green('graphcool help')} COMMAND for more information on a command.

${chalk.dim('Examples:')}

${chalk.gray('-')} Initialize a new Graphcool project
  ${chalk.green('$ graphcool init')}

${chalk.gray('-')} Update live project with local changes
  ${chalk.green('$ graphcool deploy')}
`)
  }

  listCommandsHelp(topic: string, commands: Array<typeof Command>) {
    commands = commands.filter(c => !c.hidden)
    if (commands.length <= 1) {
      return
    }
    commands.sort(compare('command'))
    const helpCmd = this.out.color.cmd(`${this.config.bin} help ${topic} COMMAND`)
    this.out.log(`${this.config.bin} ${this.out.color.bold(topic)} commands: (get help with ${helpCmd})`)
    this.out.log(renderList(commands.map(c => c.buildHelpLine(this.config))))
    this.out.log('')
  }
}
