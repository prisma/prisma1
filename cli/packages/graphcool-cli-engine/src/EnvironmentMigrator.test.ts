import { getTmpDir } from './test/getTmpDir'
import { EnvironmentMigrator } from './EnvironmentMigrator'
import * as fs from 'fs-extra'
import * as path from 'path'
import { TestOutput } from './Output/interface'

/**
 * Tests overview
  test('.graphcool with json, non-existent backup file', () => {
  test('.graphcool with json, existing backup file', () => {
  test('.graphcool with yaml, non-existent backup file', () => {
  test('.graphcool with yaml, existing backup file', () => {
  test('.graphcool folder, non-existent backup file, non-existent .graphcoolrc', () => {
  test('.graphcool folder, non-existent backup file, existing .graphcoolrc', () => {
  test('.graphcool folder, non-existent backup file, existing .graphcoolrc', () => {
  test('.graphcoolrc folder, non-existent backup file', () => {
  test('.graphcoolrc folder, existing backup folder (or file)', () => {
  test('.graphcoolrc file with valid yaml, non-existent backup file', () => {
  test('.graphcoolrc file with valid yaml, existing backup folder (or file)', () => {
  test('.graphcoolrc file with invalid yaml, non-existent backup file', () => {
  test('.graphcoolrc file with invalid yaml, existing backup folder (or file)', () => {
 */

describe('EnvironmentMigrator', () => {
  test('.graphcool with json, non-existent backup file', () => {
    const home = getTmpDir()
    fs.outputFileSync(path.join(home, '.graphcool'), '{"example": "Json"}')

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    fs.removeSync(home)
  })
  test('.graphcool with json, existing backup file', () => {
    const home = getTmpDir()
    fs.writeFileSync(path.join(home, '.graphcool'), '{"example": "Json"}')
    fs.writeFileSync(path.join(home, '.graphcool.backup'), '')

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    fs.removeSync(home)
  })

  test('.graphcool with yaml, non-existent backup file', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcool'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })

  test('.graphcool with yaml, existing backup file', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcool'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )
    fs.outputFileSync(
      path.join(home, '.graphcool.backup'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    fs.removeSync(home)
  })

  test('.graphcool folder, non-existent backup file, non-existent .graphcoolrc', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcool/config.yml'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })
  test('.graphcool folder, non-existent backup file, existing .graphcoolrc', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcool/config.yml'),
      `clusters:
  local2:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )
    fs.outputFileSync(
      path.join(home, '.graphcoolrc'),
      `graphcool-framework:
  clusters:
    local:
      host: 'http://localhost:60000'
    remote:
      host: 'https://remote.graph.cool'
      clusterSecret: 'here-is-a-token'
graphcool-1.0:
  clusters:
    local:
      host: 'http://localhost:60002'
    remote:
      host: 'https://remote.graph.cool'
      clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })
  test('.graphcool folder, existing backup folder (or file), execute 2 times', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcool/config.yml'),
      `clusters:
  local2:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )
    fs.outputFileSync(
      path.join(home, '.graphcool.backup'),
      `graphcool-framework:
  clusters:
    local:
      host: 'http://localhost:60000'
    remote:
      host: 'https://remote.graph.cool'
      clusterSecret: 'here-is-a-token'
graphcool-1.0:
  clusters:
    local:
      host: 'http://localhost:60002'
    remote:
      host: 'https://remote.graph.cool'
      clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    // migrate 2 times
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    fs.removeSync(home)
  })

  test('.graphcoolrc folder, non-existent backup file', () => {
    const home = getTmpDir()
    fs.mkdirpSync(path.join(home, '.graphcoolrc'))

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    // migrate 2 times
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    fs.removeSync(home)
  })
  test('.graphcoolrc folder, existing backup folder (or file)', () => {
    const home = getTmpDir()
    fs.mkdirpSync(path.join(home, '.graphcoolrc'))
    fs.mkdirpSync(path.join(home, '.graphcoolrc.backup'))

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    fs.removeSync(home)
  })

  test('.graphcoolrc file with valid yaml, non-existent backup file', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcoolrc'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })
  test('.graphcoolrc file with valid yaml, existing backup folder (or file)', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcoolrc'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )
    fs.outputFileSync(
      path.join(home, '.graphcoolrc.backup'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })

  test('.graphcoolrc file with invalid yaml, non-existent backup file', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcoolrc'),
      `clusters
  local:
    host: http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })
  test('.graphcoolrc file with invalid yaml, existing backup folder (or file)', () => {
    const home = getTmpDir()
    fs.outputFileSync(
      path.join(home, '.graphcoolrc'),
      `clusters:
  local:
    host 'http://localhost:60000'
  remote:
    host: https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )
    fs.outputFileSync(
      path.join(home, '.graphcoolrc.backup'),
      `clusters:
  local:
    host: 'http://localhost:60000'
  remote:
    host: 'https://remote.graph.cool'
    clusterSecret: 'here-is-a-token'`,
    )

    const out = new TestOutput()
    const migrator = new EnvironmentMigrator(home, out)
    migrator.migrate()
    expect(out.output).toMatchSnapshot()
    expect(fs.readdirSync(home)).toMatchSnapshot()
    expect(
      fs.readFileSync(path.join(home, '.graphcoolrc'), 'utf-8'),
    ).toMatchSnapshot()
    fs.removeSync(home)
  })
})
