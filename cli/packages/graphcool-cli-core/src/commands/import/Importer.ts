import * as path from 'path'
import * as fs from 'fs-extra'
import { Client } from 'graphcool-cli-engine'
import * as globby from 'globby'
import { Validator } from './Validator'

interface Files {
  lists: string[]
  nodes: string[]
  relations: string[]
}

export class Importer {
  importPath: string
  client: Client
  types: string
  constructor(importPath: string, types: string, client: Client) {
    if (!fs.pathExistsSync(importPath)) {
      throw new Error(`Import path ${importPath} does not exist`)
    }
    this.importPath = importPath
    this.client = client
    this.types = types
  }
  async upload(projectId: string) {
    const before = Date.now()
    console.log('Uploading...')
    const files = this.getFiles()
    this.validateFiles(files)
    for (const fileName of files.nodes) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      await this.client.upload(file, projectId)
    }
    for (const fileName of files.lists) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      await this.client.upload(file, projectId)
    }
    for (const fileName of files.relations) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      await this.client.upload(file, projectId)
    }
    console.log(`Done with upload. Needed ${Date.now() - before}ms`)
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
          cwd: path.join(this.importPath, 'lists/'),
        })
        .map(p => path.join(this.importPath, 'lists/', p)),
      nodes: globby
        .sync('*.json', {
          cwd: path.join(this.importPath, 'nodes/'),
        })
        .map(p => path.join(this.importPath, 'nodes/', p)),
      relations: globby
        .sync('*.json', {
          cwd: path.join(this.importPath, 'relations/'),
        })
        .map(p => path.join(this.importPath, 'relations/', p)),
    }
  }
}
