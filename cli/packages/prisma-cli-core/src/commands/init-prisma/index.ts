import { Command, flags, Flags } from 'prisma-cli-engine'

export default class InitPrisma extends Command {
  static topic = 'init-prisma'
  static description = 'Init prisma cli'
  async run() {
    // this will later be used to init the cli for performance enhancements
  }
}
