import { Config } from './Config'
import { Output } from './Output/index'
import Plugins from './Plugin/Plugins'
import { getCommandId } from './util'

export class NotFound {
  argv: string[]
  config: Config
  out: Output
  plugins: Plugins

  constructor(output: Output, argv: string[]) {
    this.argv = argv
    this.out = output
    this.config = output.config
    this.plugins = new Plugins(output)
  }

  allCommands(): string[] {
    return this.plugins.commands.reduce((commands, c) => {
      return commands.concat([c.id] as any).concat(c.aliases || ([] as any))
    }, [])
  }

  closest(cmd: string) {
    const DCE = require('string-similarity')
    const commands = this.allCommands()
    if (commands.length > 0) {
      return DCE.findBestMatch(cmd, this.allCommands()).bestMatch.target
    }

    return null
  }

  async isValidTopic(name: string): Promise<boolean> {
    const t = await this.plugins.findTopic(name)
    return !!t
  }

  async run() {
    await this.plugins.load()

    let closest
    let binHelp = `${this.config.bin} help`
    const id = getCommandId(this.argv.slice(1))
    const idSplit = id.split(':')
    if (await this.isValidTopic(idSplit[0])) {
      // if valid topic, update binHelp with topic
      binHelp = `${binHelp} ${idSplit[0]}`
      // if topic:COMMAND present, try closest for id
      if (idSplit[1]) {
        closest = this.closest(id)
      }
    } else {
      closest = this.closest(id)
    }

    const perhaps = closest
      ? `Perhaps you meant ${this.out.color.yellow(
          closest.split(':').join(' '),
        )}\n`
      : ''
    this.out.error(
      `${this.out.color.yellow(idSplit.join(' '))} is not a ${
        this.config.bin
      } command.
${perhaps}Run ${this.out.color.cmd(binHelp)} for a list of available commands.`,
      127,
    )
  }
}
