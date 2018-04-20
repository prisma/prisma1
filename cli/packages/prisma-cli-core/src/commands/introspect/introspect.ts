import { Command, flags, Flags } from 'prisma-cli-engine'

export default class IntrospectCommand extends Command {
  static topic = 'introspect'
  static description = 'Introspect database schema(s) of service'
  static flags: Flags = {
    // stage: flags.string({
    //   char: 't',
    //   description: 'Target name',
    //   defaultValue: 'dev',
    // }),
  }
  async run() {
    let {} = this.flags
    throw new Error('Not implemented yet')

    // const { id } = await this.env.getTarget(stage)

    // continue
  }
}
