import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import * as JSONStream from 'JSONStream'
import * as through2 from 'through2'
import * as es from 'event-stream'

export default class Import extends Command {
  static topic = 'import'
  static description = 'Import data into a service'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
    source: flags.string({
      char: 's',
      description: 'Path to zip or folder including data to import',
      required: true,
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    const { target, source } = this.flags

    const { id } = await this.env.getTarget(target)

    // continue
    console.log(target)
    await this.import(source)
  }

  import(source: string) {
    console.log('reading', source)
    return new Promise((resolve, reject) => {
      fs
        .createReadStream(source)
        .pipe(JSONStream.parse('*'))
        .pipe(
          es.mapSync(data => {
            console.error(data)
            return data
          }),
        )
      // .pipe(
      //   through2.obj(function(chunk, env, callback) {
      //     debugger
      //     const data = {}
      //     this.push(data)
      //     callback()
      //   }),
      // )
    })
  }
}

const mapping = {
  'User.comments': 'User.comments.*.id',
  'Comment.user': 'userId',
}
