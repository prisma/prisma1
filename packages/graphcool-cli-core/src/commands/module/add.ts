import {Command, flags, Flags, readDefinition, fsToModule} from 'graphcool-cli-engine'
import * as download from 'download-github-repo'
import * as cuid from 'cuid'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
/* tslint:disable-next-line */
const debug = require('debug')('module')
import * as chalk from 'chalk'

export default class ModuleAdd extends Command {
  static topic = 'module'
  static command = 'add'
  static description = 'Add a new module'
  static hidden = true
  static args = [{
    name: 'moduleUrl',
    required: true,
  }]
  async run() {
    await this.definition.load()
    const moduleUrl = this.argv[1]
    const splittedModule = moduleUrl.split('/')
    const ghUser = splittedModule[0]
    const ghRepo = splittedModule[1]
    const moduleDirName = splittedModule[splittedModule.length - 1]
    const subPath = splittedModule.length > 2 ? splittedModule.slice(2).join('/') : ''

    const tmpDir = path.join(os.tmpdir(), `${cuid()}/`)
    fs.mkdirpSync(tmpDir)

    const repoName = `${ghUser}/${ghRepo}`

    this.out.action.start(`Downloading from ${repoName}`)
    await downloadRepo(repoName, tmpDir)
    this.out.action.stop(`Done downloading ${repoName}`)
    debug(`Downloaded ${repoName} to ${tmpDir}`)

    const source = path.join(tmpDir, subPath)
    const relativeModulePath = `./modules/${moduleDirName}/`
    const target = path.join(this.config.definitionDir, relativeModulePath)
    if (fs.pathExistsSync(target)) {
      this.out.warn(`Path ${target} already exists. Overwriting it now.`)
    }
    fs.mkdirpSync(target)
    debug(`Copying from ${source} to ${target}`)
    fs.copySync(source, target)
    fs.removeSync(source)

    // add it to local definition file

    debug('Done!')

    const rootDefinitionString = this.definition.definition!.modules[0].content
    const rootDefinition = await readDefinition(rootDefinitionString, this.out)
    debug('setting module', moduleDirName, relativeModulePath)
    rootDefinition.modules[moduleDirName] = path.join(relativeModulePath, 'graphcool.yml')
    const file = yaml.safeDump(rootDefinition)
    fs.writeFileSync(path.join(this.config.definitionDir, 'graphcool.yml'), file)
    debug('Added module to graphcool.yml')

    const module = await fsToModule(target, this.out)
    this.definition.definition!.modules.push({
      ...module,
      name: moduleDirName,
    })

    this.out.log(`Successfully added module ${moduleDirName}. You now can run ${chalk.bold('graphcool deploy')} to deploy changes`)
  }
}

function downloadRepo(repo: string, destination: string) {
  return new Promise((resolve, reject) => {
    download(repo, destination, (err) => {
      if (err) {
        reject(err)
      } else {
        resolve()
      }
    })
  })
}
