import { Command, flags, Flags, readDefinition } from 'graphcool-cli-engine'
import * as download from 'download-github-repo'
import * as cuid from 'scuid'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import * as childProcess from 'child_process'
/* tslint:disable-next-line */
const debug = require('debug')('module')
import chalk from 'chalk'
import * as figures from 'figures'
import {intersection, difference} from 'lodash'
import { getBinPath } from './getbin'
import 'isomorphic-fetch'

export default class AddTemplate extends Command {
  static topic = 'add-template'
  static description = 'Add a new template'
  static args = [
    {
      name: 'templateUrl',
      required: true,
    },
  ]
  static help = `
  
 ${chalk.green.bold('Examples:')}
      
  ${chalk.bold('Github Authentication')}
  $ ${chalk.cyan(
    'graphcool add-template auth/github',
  )}

  ${chalk.bold('Facebook Authentication')}
  $ ${chalk.cyan(
    'graphcool add-template auth/facebook',
  )}

  ${chalk.bold('Send mails with Mailgun')}
  $ ${chalk.cyan(
    'graphcool add-template messaging/mailgun',
  )}

  ${chalk.bold('Send SMS with Twilio')}
  $ ${chalk.cyan(
    'graphcool add-template messaging/twilio',
  )}
  
  All templates:
  https://github.com/graphcool/templates
  `
  async run() {
    await this.definition.load(this.flags)
    const moduleUrl = this.argv[0]

    const {repoName, subPath, moduleDirName} = await this.extractInfo(moduleUrl)

    const tmpDir = path.join(os.tmpdir(), `${cuid()}/`)
    fs.mkdirpSync(tmpDir)

    await this.checkUrl(repoName, subPath, moduleUrl)

    this.out.action.start(
      `Downloading template ${chalk.bold.cyan(moduleUrl)} from ${chalk.bold(
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
    const relativeModulePath = `./src/${moduleDirName}/`
    const target = path.join(this.config.definitionDir, relativeModulePath)
    if (fs.pathExistsSync(target)) {
      this.out.log(`Path ${target} already exists. Overwriting it now.`)
    }
    fs.mkdirpSync(target)
    fs.copySync(path.join(source, './src'), target)

    // add it to local definition file
    const newModulePath = path.join(source, 'graphcool.yml')
    const templateYml = fs.readFileSync(newModulePath, 'utf-8')
    const newTemplateYml = templateYml.replace(/src\//g, `src/${moduleDirName}/`)

    const templateTypesRelativePath = yaml.safeLoad(newTemplateYml).types
    const templateTypesPath = path.join(source, templateTypesRelativePath)
    const templateTypes = fs.readFileSync(templateTypesPath, 'utf-8')

    const newDefinition = this.definition.mergeDefinition(newTemplateYml, moduleDirName)
    const newTypes = this.definition.mergeTypes(templateTypes, moduleDirName)
    const typesPath = this.definition.definition!.modules[0].definition!.types

    await this.mergePackageJsons(source)

    fs.removeSync(source)

    fs.writeFileSync(
      path.join(this.config.definitionDir, 'graphcool.yml'),
      newDefinition,
    )
    fs.writeFileSync(
      path.join(this.config.definitionDir, typesPath),
      newTypes,
    )

    this.out.log('')
    this.out.log(
      chalk.blue(
        `${chalk.bold('Added')} functions & permissions comments of template ${chalk.bold(
          moduleDirName,
        )} to ${chalk.bold('graphcool.yml')}`,
      ),
    )
    this.out.log(
      chalk.blue(
        `${chalk.bold('Added')} type comments of template ${chalk.bold(
          moduleDirName,
        )} to ${chalk.bold(typesPath)}`,
      ),
    )
    this.out.log(chalk.blue.bold(`Created ${relativeModulePath}:`))
    this.out.tree(relativeModulePath, false)


    const readmePath = path.join(target, 'USAGE.md')
    if (fs.pathExistsSync(readmePath)) {
      let readme = fs.readFileSync(readmePath, 'utf-8')
      try {
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

    this.out.log(`Please have a look in the ${chalk.green('graphcool.yml')} and ${chalk.green('types.graphql')} and ${chalk.bold('comment out')} the added template comments there.`)

    // this.out.log(
    //   `   ${chalk.green(figures.tick)} You now can run ${chalk.bold(
    //     'graphcool deploy',
    //   )} to deploy changes`,
    // )
  }

  async extractInfo(moduleUrl: string): Promise<{repoName: string, subPath: string, moduleDirName: string}> {
    let splittedModule = moduleUrl.split('/')

    const whiteList = ['auth', 'misc', 'messaging']

    if (splittedModule.length === 1 || whiteList.includes(splittedModule[0])) {
      const res = await fetch('https://raw.githubusercontent.com/graphcool/templates/master/templates.json')
      const templates = await res.json()
      if (!templates[moduleUrl]) {
        this.out.error(`Could not find template ${chalk.bold(moduleUrl)}.
Check https://github.com/graphcool/templates for official templates.`)
      }

      splittedModule = `graphcool/templates/${templates[moduleUrl].path}`.split('/')
    }

    const ghUser = splittedModule[0]
    const ghRepo = splittedModule[1]
    const repoName = `${ghUser}/${ghRepo}`

    const moduleDirName = splittedModule[splittedModule.length - 1]
    const subPath =
      splittedModule.length > 2 ? splittedModule.slice(2).join('/') : ''

    return {
      repoName, subPath, moduleDirName
    }
  }

  async mergePackageJsons(source: string) {
    debug('going to merge packagejsons', source)
    const sourcePjsonPath = path.join(source, 'package.json')
    const destPjsonPath = path.join(this.config.definitionDir, 'package.json')
    debug('source', sourcePjsonPath)
    debug('dest', destPjsonPath)
    debugger
    if (fs.pathExistsSync(sourcePjsonPath)) {
      if (fs.pathExistsSync(destPjsonPath)) {
        try {
          const templateJson = fs.readJSONSync(sourcePjsonPath)
          const serviceJson = fs.readJSONSync(destPjsonPath)
          const templateDeps: any = templateJson.dependencies || {}
          const serviceDeps: any = serviceJson.dependencies || {}
          const intersect = intersection(Object.keys(serviceDeps), Object.keys(templateDeps))
          const conflicts = intersect.filter(name => {
            return templateDeps[name] !== serviceDeps[name]
          })
          if (conflicts.length > 0) {
            this.out.warn(`There are conflicts in dependencies for the template package.json and package.json of the current service:`)
            this.out.warn(conflicts.join(', '))
            this.out.log('Please resolve them by hand. This is the templates package.json:')
            this.out.log(this.out.getStyledJSON(templateJson))
          }
          const newDependencies = difference(Object.keys(templateDeps), Object.keys(serviceDeps))
          if (newDependencies.length > 0) {
            if (!serviceJson.dependencies) {
              serviceJson.dependencies = {}
            }
            newDependencies.forEach(name => {
              serviceJson.dependencies[name] = templateDeps[name]
            })
            if (newDependencies.length === 1) {
              this.out.log(`The dependency ${chalk.bold(newDependencies.join(', '))} has been added to the main package.json`)
            } else if (newDependencies.length > 1) {
              this.out.log(`The dependencies ${chalk.bold(newDependencies.join(', '))} have been added to the main package.json`)
            }
            const newJson = JSON.stringify(serviceJson, null, 2)
            fs.writeFileSync(destPjsonPath, newJson)
            this.out.log(`Written ${chalk.bold(destPjsonPath)}\n`)
            await this.npmInstall()
          }
          if (conflicts.length === 0 && newDependencies.length === 0) {
            this.out.log(`${chalk.bold('package.json')}: No new dependencies needed.`)
          }
        } catch (e) {
          this.out.warn(e)
        }
      } else {
        this.out.log(`There is no package.json yet, so the templates' package.json has been copied`)
        fs.copySync(sourcePjsonPath, destPjsonPath)
        await this.npmInstall()
      }

    } else {
      this.out.log('Path does not exist')
    }
  }

  private async checkUrl(repoName, subPath, moduleUrl) {
    const githubUrl = `https://github.com/${repoName.split('#')[0]}/tree/master/${subPath}`

    debug('fetching', githubUrl)
    const result = await fetch(githubUrl)
    if (result.status === 404) {
      this.out.error(`Could not find ${moduleUrl}. Please check if the github repository ${githubUrl} exists`)
    }
  }

  private npmInstall(): Promise<void> {
    return new Promise(async (resolve, reject) => {
      const cmdPath = await getBinPath('yarn') || await getBinPath('npm')
      const child = childProcess.spawn(cmdPath!, ['install'], {
        cwd: this.config.definitionDir,
      })
      child.stdout.pipe(process.stdout)
      child.stderr.pipe(process.stderr)
      child.on('error', err => {
        this.out.error(err)
      })
      child.on('close', code => {
        if (code !== 0) {
          reject(code)
        } else {
          resolve()
        }
      })
    })
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
