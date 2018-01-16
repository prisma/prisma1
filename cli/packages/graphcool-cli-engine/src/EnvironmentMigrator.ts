import * as path from 'path'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import { merge } from 'lodash'
import { IOutput, Output } from './Output/interface'
const debug = require('debug')('EnvironmentMigrator')

const readMore = `. Read more here: https://goo.gl/3bLCVV`

export class EnvironmentMigrator {
  home: string
  out: IOutput
  graphcoolPath: string
  graphcoolBackupPath: string
  rcPath: string
  rcBackupPath: string
  constructor(home: string, out: IOutput = new Output()) {
    this.home = home
    this.out = out
    this.graphcoolPath = path.join(this.home, '.graphcool')
    this.graphcoolBackupPath = path.join(this.home, '.graphcool.backup')
    this.rcBackupPath = path.join(this.home, '.graphcoolrc.backup')
    this.rcPath = path.join(this.home, '.graphcoolrc')
  }
  migrate() {
    if (fs.pathExistsSync(this.graphcoolPath)) {
      const isFile = fs.lstatSync(this.graphcoolPath).isFile()
      if (isFile) {
        this.migrateGraphcoolFile()
      } else {
        this.migrateGraphcoolFolder()
      }
      debug(`graphcoolPath ${this.graphcoolPath} does exists`)
    } else {
      debug(`graphcoolPath ${this.graphcoolPath} does not exist`)
    }
    if (fs.pathExistsSync(this.rcPath)) {
      const isFile = fs.lstatSync(this.rcPath).isFile()
      if (isFile) {
        this.migrateRC()
      } else {
        this.migrateRCFolder()
      }
    }
  }
  migrateGraphcoolFile() {
    const file = fs.readFileSync(this.graphcoolPath, 'utf-8')
    // only migrate yaml, if it's not json
    try {
      const content = JSON.parse(file)
      this.migrateGraphcoolJsonFile()
    } catch (e) {
      try {
        const content = yaml.safeLoad(file)
        this.migrateGraphcoolYamlFile(content)
      } catch (e) {
        //
      }
    }
  }
  migrateGraphcoolYamlFile(content: any) {
    this.mergeAndSaveGraphcoolRC({
      'graphcool-1.0': content,
    })
    this.moveGraphcoolFile()
    this.out.log('Its content has been migrated to ~/.graphcoolrc')
  }
  mergeAndSaveGraphcoolRC(content: any) {
    const fileExists = fs.pathExistsSync(this.rcPath)
    const file = fileExists ? fs.readFileSync(this.rcPath, 'utf-8') : null
    let currentRC = file ? yaml.safeLoad(file) : {}
    currentRC = currentRC.clusters || currentRC.platformToken ? {} : currentRC
    const newRC = merge(currentRC, content)
    this.saveGraphcoolRC(newRC)
  }
  saveGraphcoolRC(content: any) {
    const rcString = yaml.safeDump(JSON.parse(JSON.stringify(content)))
    fs.writeFileSync(this.rcPath, rcString)
  }
  migrateGraphcoolJsonFile() {
    this.moveGraphcoolFile()
  }
  migrateGraphcoolFolder() {
    const configPath = path.join(this.home, '.graphcool/config.yml')
    if (fs.pathExistsSync(configPath)) {
      const file = fs.readFileSync(configPath, 'utf-8')
      try {
        const content = yaml.safeLoad(file)
        this.mergeAndSaveGraphcoolRC({
          'graphcool-1.0': content,
        })
        this.out.warn(
          `Moved content of ~/.graphcool/config.yml to ~/.graphcoolrc${readMore}`,
        )
      } catch (e) {
        //
      }
    }
    if (!fs.pathExistsSync(this.graphcoolBackupPath)) {
      fs.moveSync(this.graphcoolPath, this.graphcoolBackupPath)
      this.out.warn(
        `~/.graphcool/ folder found. It has been moved to ~/.graphcool.backup.`,
      )
    } else {
      this.out
        .warn(`~/.graphcool/ folder found. It could not be moved, as ~/.graphcool.backup already exists.
Please remove ~/.graphcool by hand to prevent this warning.`)
    }
  }
  migrateRC() {
    const rc = fs.readFileSync(this.rcPath, 'utf-8')
    let success = false
    try {
      const content = yaml.safeLoad(rc)
      if (!this.hasCorrectFormat(content)) {
        this.mergeAndSaveGraphcoolRC({
          'graphcool-framework': content,
        })
        success = true
      } else {
        // if there are accidentally platformToken or clusters on the top-level, put it into the graphcool namespace
        if (content.platformToken || content.clusters) {
          const newContent = {
            ['graphcool-framework']: {
              ...content['graphcool-framework'],
              platformToken: content.platformToken,
              clusters: content.clusters,
            },
            ['graphcool-1.0']: content['graphcool-1.0'],
          }
          this.saveGraphcoolRC(newContent)
          this.out.warn(`~/.graphcoolrc has been updated`)
        }
        return
      }
    } catch (e) {
      //
    }
    if (!fs.pathExistsSync(this.rcBackupPath)) {
      fs.writeFileSync(this.rcBackupPath, rc)
      this.out.warn(
        `Old ~/.graphcoolrc file found. It has been backed up to ~/.graphcoolrc.backup.`,
      )
    } else {
      this.out
        .warn(`~/.graphcoolrc file found. It could not be moved, as ~/.graphcoolrc.backup already exists.
Please remove ~/.graphcoolrc by hand to prevent this warning.`)
    }
    if (success) {
      this.out.log(`Its content has been migrated to ~/.graphcoolrc`)
    }
  }
  migrateRCFolder() {
    this.moveRCFolder()
  }
  moveGraphcoolFile() {
    if (!fs.pathExistsSync(this.graphcoolBackupPath)) {
      fs.moveSync(this.graphcoolPath, this.graphcoolBackupPath)
      this.out.warn(
        `Old ~/.graphcool file found. It has been moved to ~/.graphcool.backup.`,
      )
    } else {
      this.out
        .warn(`Old ~/.graphcool file found. It could not be moved, as ~/.graphcool.backup already exists.
Please remove ~/.graphcool by hand to prevent this warning.`)
    }
  }
  moveRCFolder() {
    if (!fs.pathExistsSync(this.rcBackupPath)) {
      fs.moveSync(this.rcPath, this.rcBackupPath)
      this.out.warn(
        `~/.graphcoolrc/ folder found. It has been moved to ~/.graphcoolrc.backup/.`,
      )
    } else {
      this.out
        .warn(`~/.graphcoolrc folder found. It could not be moved, as ~/.graphcoolrc.backup already exists.
Please remove ~/.graphcoolrc/ by hand to prevent this warning.`)
    }
  }
  moveRCFile() {
    if (!fs.pathExistsSync(this.rcBackupPath)) {
      fs.moveSync(this.rcPath, this.rcBackupPath)
      this.out.warn(
        `~/.graphcoolrc file found. It has been moved to ${this.rcBackupPath}.`,
      )
    } else {
      this.out
        .warn(`~/.graphcoolrc file found. It could not be moved, as ~/.graphcoolrc.backup already exists.
Please remove ~/.graphcoolrc by hand to prevent this warning.`)
    }
  }
  hasCorrectFormat(content: any) {
    if (!content) {
      return false
    }

    if (Object.keys(content).length > 0) {
      return content['graphcool-1.0'] || content['graphcool-framework']
    }
  }
}
