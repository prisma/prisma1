import * as cuid from 'scuid'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import {Config} from './Config'

const mockDotFile = {
  "token": "test-token"
}


describe('config', () => {
  test('should init paths correct in subfolder', async () => {
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
  test('should allow .graphcoolrc file in current folder', async () => {
    const home = path.join(os.tmpdir(), `${cuid()}`)
    const definitionDir = path.join(home, 'definition')
    const cwd = path.join(definitionDir, 'src')

    fs.mkdirpSync(definitionDir)
    fs.mkdirpSync(home)
    fs.copySync(path.join(__dirname, '../test/test-project'), definitionDir)
    const dotGraphcoolPath = path.join(definitionDir, '.graphcoolrc')
    fs.writeFileSync(dotGraphcoolPath, JSON.stringify(mockDotFile))

    const config = new Config({mock: true, home, cwd})
    expect(config.localRCPath).toBe(dotGraphcoolPath)
  })
})
