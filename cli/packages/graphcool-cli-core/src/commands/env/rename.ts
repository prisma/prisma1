import {Command, flags, Flags} from 'graphcool-cli-engine'

export default class RenameEnv extends Command {
  static topic = 'env'
  static command = 'rename'
  static description = 'Renames a project environment'
  static flags: Flags = {
    oldName: flags.string({
      char: 'o',
      description: 'Old name',
      required: true
    }),
    newName: flags.string({
      char: 'n',
      description: 'New name',
      required: true
    }),
  }
  async run() {
    const {oldName, newName} = this.flags

    this.env.rename(oldName, newName)
    this.env.save()
    this.out.log(`Renamed environment ${oldName} to ${newName}`)
  }
}
