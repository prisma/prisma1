import { graphcoolRCFilePath } from '../utils/constants'
import * as fs from 'fs'

class GraphcoolRC {
  token: string | null = null

  setToken(token: string) {
    this.token = token
  }

  unsetToken() {
    this.token = null
  }

  load() {
    if (fs.existsSync(graphcoolRCFilePath)) {
      const configContent = fs.readFileSync(graphcoolRCFilePath, 'utf-8')
      this.token = JSON.parse(configContent).token
    }
  }

  save() {
    const json = JSON.stringify({token: this.token}, null, 2)
    fs.writeFileSync(graphcoolRCFilePath, json)
  }
}

// expose it as a singleton
const config = new GraphcoolRC()
config.load()

export default config
