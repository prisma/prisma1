import { Command, flags, Flags } from '../../../../prisma-cli-engine/dist'
const debug = require('debug')('logout')

export default class Logout extends Command {
  static topic = 'logout'
  static description = 'Logout from Prisma Cloud'

  async run() {
    this.client.logout()
    console.log('You have been successfully logged out.')
  }
}
