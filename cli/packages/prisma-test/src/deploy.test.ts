import { spawnSync } from 'npm-run'
import * as rimraf from 'rimraf'
import { setupMockEnvironment, REGION } from './TestHelper'

describe('deploy, test, and delete', () => {
  const { cwd: cwdEU, endpoint: endpointEU } = setupMockEnvironment({
    region: REGION.EU,
  })

  const { cwd: cwdUS, endpoint: endpointUS } = setupMockEnvironment({
    region: REGION.US,
  })

  test('deploy a service in EU', async () => {
    const deployCmd = spawnSync('prisma', ['deploy'], { cwd: cwdEU })
    expect(
      deployCmd.stdout.toString().indexOf(`HTTP:  ${endpointEU}`) > -1,
    ).toBe(true)
    expect(deployCmd.status).toBe(0)
  })

  test('delete the EU service', async () => {
    const deleteCmd = spawnSync('prisma', ['delete', '-f'], { cwd: cwdEU })
    expect(
      deleteCmd.stderr.toString().indexOf(`Deleting service`) >
        -1,
    ).toBe(true)
    expect(deleteCmd.status).toBe(0)
    rimraf.sync(cwdEU)
  })

  test('deploy a service in US', async () => {
    const deployCmd = spawnSync('prisma', ['deploy'], { cwd: cwdUS })
    expect(
      deployCmd.stdout.toString().indexOf(`HTTP:  ${endpointUS}`) > -1,
    ).toBe(true)
    expect(deployCmd.status).toBe(0)
  })

  test('delete the US service', async () => {
    const deleteCmd = spawnSync('prisma', ['delete', '-f'], { cwd: cwdUS })
    expect(
      deleteCmd.stderr.toString().indexOf(`Deleting service`) >
        -1,
    ).toBe(true)
    expect(deleteCmd.status).toBe(0)
    rimraf.sync(cwdUS)
  })
})
