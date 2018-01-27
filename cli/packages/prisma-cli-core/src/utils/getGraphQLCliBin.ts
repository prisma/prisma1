import * as path from 'path'
import * as fs from 'fs-extra'
import { getBinPath } from '../commands/deploy/getbin'

export default async function getGraphQLCliBin(): Promise<string> {
  let binPath = path.join(
    __dirname,
    '../../node_modules/graphql-cli/dist/bin.js',
  )

  if (!fs.pathExistsSync(binPath)) {
    binPath = path.join(
      __dirname,
      '../../../../node_modules/graphql-cli/dist/bin.js',
    )
  }

  if (!fs.pathExistsSync(binPath)) {
    binPath = path.join(__dirname, '../../../graphql-cli/dist/bin.js')
  }

  if (!fs.pathExistsSync(binPath)) {
    binPath = (await getBinPath('graphql')) || 'graphql'
  }

  return binPath
}
