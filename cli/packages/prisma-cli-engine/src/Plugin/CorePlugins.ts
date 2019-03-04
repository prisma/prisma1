import * as path from 'path'
import { Manager } from './Manager'
import { PluginPath } from './PluginPath'
import * as fs from 'fs-extra'

export default class CorePlugins extends Manager {
  /**
   * list core plugins
   * @returns {PluginPath[]}
   */
  async list(): Promise<PluginPath[]> {
    try {
      const cli = this.config.pjson['cli-engine']
      let plugins: any[] = []
      // TODO maybe enable later, but for now we only want plugins from the core-plugins package
      // if (this.config.pjson.main) {
      //   // if main is set in package.json, add plugin as self
      //   plugins.push(new PluginPath({output: this.out, type: 'core', path: this.config.root}))
      // }
      if (!cli) {
        return plugins
      }
      if (cli.plugins) {
        plugins = plugins.concat(
          (cli.plugins || []).map(name => {
            let pluginPath = path.join(this.config.root, 'node_modules', name)
            if (!fs.pathExistsSync(pluginPath)) {
              pluginPath = path.join(
                this.config.root,
                '../../',
                'node_modules',
                name,
              )
            }
            return new PluginPath({
              output: this.out,
              type: 'core',
              path: pluginPath,
            })
          }),
        )
      }
      return plugins
    } catch (err) {
      this.out.warn(err, 'Error loading core plugins')
      return []
    }
  }
}
