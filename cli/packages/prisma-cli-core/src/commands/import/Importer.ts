import * as path from 'path'
import * as fs from 'fs-extra'
import { Client, Output, Config } from 'prisma-cli-engine'
import * as globby from 'globby'
import { Validator } from './Validator'
import chalk from 'chalk'
import * as unzip from 'unzip'

export interface Files {
  lists: string[]
  nodes: string[]
  relations: string[]
}

export interface State {
  lists: number
  nodes: number
  relations: number
}

const defaultState = {
  nodes: 0,
  lists: 0,
  relations: 0,
}

export class Importer {
  importPath: string
  importDir: string
  client: Client
  types: string
  out: Output
  statePath: string
  config: Config
  constructor(
    importPath: string,
    types: string,
    client: Client,
    out: Output,
    config: Config,
  ) {
    if (!fs.pathExistsSync(importPath)) {
      throw new Error(`Import path ${importPath} does not exist`)
    }
    this.config = config
    this.importPath = importPath
    this.importDir = path.join(config.cwd, '.import/')
    this.client = client
    this.types = types
    this.out = out
    this.statePath = path.join(this.importDir, 'state.json')
  }
  saveState(state) {
    fs.writeFileSync(this.statePath, JSON.stringify(state))
  }
  getState(): State {
    try {
      const json = fs.readJsonSync(this.statePath)
      return {
        ...defaultState,
        ...json,
      }
    } catch (e) {
      //
    }

    return defaultState
  }
  getNumber(fileName: string) {
    const fileRegex = /.*?(\d+)\.json/
    const match = fileName.match(fileRegex)
    if (match) {
      return parseInt(match[1], 10)
    }

    return 0
  }
  unzip(): Promise<void> {
    return new Promise((resolve, reject) => {
      const before = Date.now()
      this.out.action.start('Unzipping')
      const output = unzip.Extract({ path: this.importDir })
      fs.createReadStream(this.importPath).pipe(output)
      output.on('close', () => {
        this.out.action.stop(chalk.cyan(`${Date.now() - before}ms`))
        resolve()
      })
    })
  }
  async upload(
    serviceName: string,
    stage: string,
    token?: string,
    workspaceSlug?: string,
  ) {
    await this.unzip()
    let before = Date.now()
    this.out.action.start('Validating data')
    const files = await this.getFiles()
    this.validateFiles(files)
    this.out.action.stop(chalk.cyan(`${Date.now() - before}ms`))
    before = Date.now()
    this.out.log('\nUploading nodes...')
    const state = this.getState()

    for (const fileName of files.nodes) {
      const n = this.getNumber(fileName)
      if (state.nodes >= n) {
        this.out.log(`Skipping file ${fileName} (already imported)`)
        continue
      }
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      const result = await this.client.upload(
        serviceName,
        stage,
        file,
        token,
        workspaceSlug,
      )
      if (result.length > 0) {
        this.out.log(this.out.getStyledJSON(result))
        this.out.exit(1)
      }

      state.nodes = n
      this.saveState(state)
    }
    this.out.log(
      'Uploading nodes done ' + chalk.cyan(`${Date.now() - before}ms`),
    )
    before = Date.now()
    this.out.log('\nUploading lists')
    for (const fileName of files.lists) {
      const n = this.getNumber(fileName)
      if (state.lists >= n) {
        this.out.log(`Skipping file ${fileName} (already imported)`)
        continue
      }
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      const result = await this.client.upload(
        serviceName,
        stage,
        file,
        token,
        workspaceSlug,
      )
      if (result.length > 0) {
        this.out.log(this.out.getStyledJSON(result))
        this.out.exit(1)
      }
      state.lists = n
      this.saveState(state)
    }
    this.out.log(
      'Uploading lists done ' + chalk.cyan(`${Date.now() - before}ms`),
    )
    before = Date.now()
    this.out.log('\nUploading relations')
    for (const fileName of files.relations) {
      const n = this.getNumber(fileName)
      if (state.relations >= n) {
        this.out.log(`Skipping file ${fileName} (already imported)`)
        continue
      }
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      const result = await this.client.upload(
        serviceName,
        stage,
        file,
        token,
        workspaceSlug,
      )
      if (result.length > 0) {
        this.out.log(this.out.getStyledJSON(result))
        this.out.exit(1)
      }
      state.relations = n
      this.saveState(state)
    }
    this.saveState(defaultState)
    this.out.log(
      'Uploading relations done ' + chalk.cyan(`${Date.now() - before}ms`),
    )
    fs.removeSync(this.importDir)
  }

  validateFiles(files: Files) {
    const validator = new Validator(this.types)
    for (const fileName of files.nodes) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      validator.validateImportData(json)
    }
    for (const fileName of files.lists) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      validator.validateImportData(json)
    }
    for (const fileName of files.relations) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      validator.validateImportData(json)
    }
  }

  getFiles(): Files {
    return {
      lists: globby
        .sync('*.json', {
          cwd: path.join(this.importDir, 'lists/'),
        })
        .map(p => path.join(this.importDir, 'lists/', p)),
      nodes: globby
        .sync('*.json', {
          cwd: path.join(this.importDir, 'nodes/'),
        })
        .map(p => path.join(this.importDir, 'nodes/', p)),
      relations: globby
        .sync('*.json', {
          cwd: path.join(this.importDir, 'relations/'),
        })
        .map(p => path.join(this.importDir, 'relations/', p)),
    }
  }
}
