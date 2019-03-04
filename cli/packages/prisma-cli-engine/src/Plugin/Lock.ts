import 'source-map-support/register'
import * as lock from './rwlockfile'
import { Config } from '../Config'
import * as path from 'path'
import { Output } from '../Output/index'

export default class Lock {
  config: Config
  out: Output

  constructor(output: Output) {
    this.out = output
    this.config = output.config as any
  }

  get updatelockfile(): string {
    return path.join(this.config.cacheDir, 'update.lock')
  }

  // get read lock
  async read() {
    return lock.read(this.updatelockfile)
  }

  async unread() {
    await lock.unread(this.updatelockfile)
  }

  async canRead() {
    const hasWriter = await lock.hasWriter(this.updatelockfile)
    return !hasWriter
  }

  // upgrade to writer
  async upgrade() {
    // take off reader
    await this.unread()

    // check for other readers
    if (await lock.hasReaders(this.updatelockfile)) {
      this.out.action.status = `Waiting for all commands to finish`
    }

    // grab writer lock
    const unlock = await lock.write(this.updatelockfile)

    // return downgrade function
    return async () => {
      // turn back into reader when unlocking
      await unlock()
      return lock.read(this.updatelockfile)
    }
  }
}
