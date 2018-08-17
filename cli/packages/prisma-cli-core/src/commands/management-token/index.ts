import { Command, flags, Flags } from 'prisma-cli-engine'
import * as ncp from 'copy-paste'

export default class ManagementToken extends Command {
  static topic = 'management-token'
  static description = 'Create a new management API token'
  static flags: Flags = {
    secret: flags.string({
      description: 'Management API secret',
      char: 's',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
    ['expiry-duration']: flags.number({
      description: 'An expiry duration in hours',
      char: 'd',
    }),
    copy: flags.boolean({
      description: 'Copy token to clipboard',
      char: 'c',
    }),
  }
  async run() {
    const { secret, copy } = this.flags
    const envFile = this.flags['env-file']
    const expDuration = this.flags['expiry-duration']
    const tokenPayload = {
      grants: [
        {
          target: '*/*',
          action: '*',
        },
      ],
    }
  }
}
