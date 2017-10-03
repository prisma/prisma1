import * as cuid from 'cuid'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import {Config} from './Config'

describe('config', () => {
  test.only('should init paths correct in subfolder', async () => {
    const home = path.join(os.tmpdir(), `${cuid()}`)
    const definitionDir = path.join(os.tmpdir(), `${cuid()}`)
    const cwd = path.join(definitionDir, 'src')

    fs.mkdirpSync(definitionDir)
    fs.mkdirpSync(home)
    fs.copySync(path.join(__dirname, '../test/test-project'), definitionDir)

    const config = new Config({mock: true, home, cwd})
    expect(config.definitionDir).toBe(definitionDir)
    expect(config.definitionPath).toBe(path.join(definitionDir, 'graphcool.yml'))
  })
})
