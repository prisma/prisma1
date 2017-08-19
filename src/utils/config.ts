import { graphcoolConfigFilePath } from './constants'
import * as fs from 'fs'

class Config {
  token: string

  setToken(token: string) {
    this.token = token
  }

  load() {
    if (fs.existsSync(graphcoolConfigFilePath)) {
      const configContent = fs.readFileSync(graphcoolConfigFilePath, 'utf-8')
      this.token = JSON.parse(configContent).token
    }
  }

  save() {
    const json = JSON.stringify({token: this.token}, null, 2)
    fs.writeFileSync(graphcoolConfigFilePath, json)
  }
}

// expose it as a singleton
const config = new Config()
config.load()

export default config
