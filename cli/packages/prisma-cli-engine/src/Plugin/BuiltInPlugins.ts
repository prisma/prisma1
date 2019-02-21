import * as path from 'path'
import { Manager } from './Manager'
import { PluginPath } from './PluginPath'

export default class BuiltinPlugins extends Manager {
  /**
   * list builtin plugins
   * @returns {PluginPath[]}
   */
  async list(): Promise<PluginPath[]> {
    const commandsPath = path.resolve(path.join(__dirname, '..', 'commands'))
    return [
      new PluginPath({ output: this.out, type: 'builtin', path: commandsPath }),
    ]
  }
}
