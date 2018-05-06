import { getBinPath } from '../commands/deploy/getbin'
import * as childProcess from 'child_process'

export async function isDockerComposeInstalled(): Promise<string | null> {
  const bin = getBinPath('docker-compose')
  if (!bin) {
    return null
  }

  let output
  try {
    output = childProcess.execSync('docker-compose -v').toString()
  } catch (e) {
    return null
  }
  const regex = /.*?(\d{1,2}\.\d{1,2}\.\d{1,2}),?/
  const match = output.match(regex)
  if (match && match[1]) {
    const version = match[1]
    return version
  }

  return null
}
