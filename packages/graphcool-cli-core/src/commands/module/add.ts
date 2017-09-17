import { Command, flags, Flags, readDefinition } from 'graphcool-cli-engine'
import * as download from 'download-github-repo'
import * as cuid from 'cuid'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
/* tslint:disable-next-line */
const debug = require('debug')('module')
import * as chalk from 'chalk'
import * as figures from 'figures'

export default class ModuleAdd extends Command {
  static topic = 'module'
  static command = 'add'
  static description = 'Add a new module'
  static args = [
    {
      name: 'moduleUrl',
      required: true,
    },
  ]
  static help = `
  
 ${chalk.green.bold('Examples:')}
      
  ${chalk.bold('Github Authentication')}
  $ ${chalk.cyan(
    'graphcool module add graphcool/modules/authentication/github',
  )}

  ${chalk.bold('Facebook Authentication')}
  $ ${chalk.cyan(
    'graphcool module add graphcool/modules/authentication/facebook',
  )}
  
  ${chalk.bold('Algolia Syncing')}
  $ ${chalk.cyan('graphcool module add graphcool/modules/syncing/algolia')}
  `
  async run() {
    await this.definition.load()
    const moduleUrl = this.argv[1]
    const splittedModule = moduleUrl.split('/')
    const ghUser = splittedModule[0]
    const ghRepo = splittedModule[1]
    const moduleDirName = splittedModule[splittedModule.length - 1]
    const subPath =
      splittedModule.length > 2 ? splittedModule.slice(2).join('/') : ''

    const tmpDir = path.join(os.tmpdir(), `${cuid()}/`)
    fs.mkdirpSync(tmpDir)

    const repoName = `${ghUser}/${ghRepo}`

    this.out.log('')
    this.out.action.start(
      `   Downloading module ${chalk.bold.cyan(moduleUrl)} from ${chalk.bold(
        repoName,
      )} `,
    )
    try {
      await downloadRepo(repoName, tmpDir)
    } catch (e) {
      if (e === 404) {
        this.out.error(`Github repository ${repoName} could not be found`)
      }
      this.out.error(e)
    }
    this.out.action.stop()
    debug(`Downloaded ${repoName} to ${tmpDir}`)

    const source = path.join(tmpDir, subPath)
    const relativeModulePath = `./modules/${moduleDirName}/`
    const target = path.join(this.config.definitionDir, relativeModulePath)
    if (fs.pathExistsSync(target)) {
      this.out.warn(`Path ${target} already exists. Overwriting it now.`)
    }
    fs.mkdirpSync(target)
    fs.copySync(source, target)
    fs.removeSync(source)

    // add it to local definition file
    const newModulePath = path.join(relativeModulePath, 'graphcool.yml')
    const file = this.definition.insertModule(moduleDirName, newModulePath)
    fs.writeFileSync(
      path.join(this.config.definitionDir, 'graphcool.yml'),
      file,
    )
    this.out.log('')
    this.out.log(
      chalk.blue(
        `   ${chalk.bold('Added')} module ${chalk.bold(
          moduleDirName,
        )} to ${chalk.bold('graphcool.yml')}`,
      ),
    )
    this.out.log(chalk.blue.bold(`   Created ${relativeModulePath}:`))
    this.out.tree(relativeModulePath, true)

    const readmePath = path.join(target, 'README.md')
    if (fs.pathExistsSync(readmePath)) {
      let readme = fs.readFileSync(readmePath, 'utf-8')
      try {
        readme = trimReadme(readme)
        const readmeUrl = `https://github.com/${repoName}/tree/master/${subPath}`
        this.out.log(
          '   ' + chalk.bold.underline.magenta(`Setup Instructions`) + '\n',
        )
        this.out.printMarkdown(
          readme + `\n\n[Further Instructions](${readmeUrl})`,
        )
      } catch (e) {
        // noop
      }
    }

    this.out.log(
      `   ${chalk.green(figures.tick)} You now can run ${chalk.bold(
        'graphcool deploy',
      )} to deploy changes`,
    )
  }
}

function downloadRepo(repo: string, destination: string) {
  return new Promise((resolve, reject) => {
    download(repo, destination, err => {
      if (err) {
        reject(err)
      } else {
        resolve()
      }
    })
  })
}

function trimReadme(readme: string) {
  const lines = readme.split('\n')
  const gettingStartedIndex = lines.findIndex(l =>
    l.trim().startsWith('## Getting Started'),
  )
  const cutLines = lines.slice(gettingStartedIndex)
  const configurationIndex = cutLines.findIndex(l =>
    l.trim().startsWith('## Configuration'),
  )
  const configurationLines = cutLines.slice(configurationIndex)
  const nextHeadline = configurationLines
    .slice(1)
    .findIndex(l => l.trim().startsWith('#'))
  const configuration = configurationLines.slice(0, nextHeadline)

  return (
    lines.slice(1, gettingStartedIndex).join('\n') + configuration.join('\n')
  )
}
