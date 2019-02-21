import { Command, flags, Flags } from 'prisma-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class ConsoleCommand extends Command {
  static topic = 'console'
  static description = 'Open Prisma Console in browser'
  async run() {
    const url = `https://app.prisma.io`

    this.out.log(`Opening prisma console ${url} in the browser`)
    opn(url).catch(() => {}) // Prevent `unhandledRejection` error.
  }
}
