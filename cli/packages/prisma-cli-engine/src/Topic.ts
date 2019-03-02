import { Command } from './Command'
import { Output } from './Output/index'

export class Topic {
  static topic: string
  static description?: string
  static hidden = false
  static group: string

  commands: Array<typeof Command>
  out: Output

  constructor(commands: Array<typeof Command>, out: Output) {
    this.out = out
    this.commands = commands
  }

  static get id(): string {
    return this.topic
  }
}
