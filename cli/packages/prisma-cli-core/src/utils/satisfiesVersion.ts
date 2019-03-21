import * as semver from 'semver'

function normalizeVersion(version: string) {
  version = version.replace(/-beta.*/, '').replace('-alpha', '')
  const regex = /(\d+\.\d+)/
  const match = regex.exec(version)
  if (match) {
    return match[1] + '.0'
  }
  return version
}

export function satisfiesVersion(
  questionedVersion: string,
  baseVersion: string,
) {
  return semver.satisfies(
    normalizeVersion(questionedVersion),
    `>= ${baseVersion}`,
  )
}
