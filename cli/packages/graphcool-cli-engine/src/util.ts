
import { stdtermwidth } from './Output/actions/screen'

const debug = require('debug')('util')

export function compare (...props: any[]) {
  return (a: any, b: any) => {
    for (const prop of props) {
      if (a[prop] === undefined) {
        return -1
      }
      if (b[prop] === undefined) {
        return 1
      }
      if (a[prop] < b[prop]) {
        return -1
      }
      if (a[prop] > b[prop]) {
        return 1
      }
    }
    return 0
  }
}

export function wait (ms: number, unref: boolean = false): Promise<void> {
  return new Promise(resolve => {
    const t: any = setTimeout(resolve, ms)
    if (unref && typeof t.unref === 'function') {
      t.unref()
    }
  })
}

export function timeout (p: Promise<any>, ms: number): Promise<void> {
  return Promise.race([
    p,
    wait(ms, true).then(() => debug('timed out'))
  ])
}

export function undefault (m: any) {
  return m.default ? m.default : m
}

export function linewrap(length: number, s: string): string {
  const linewrapOverride = require('@heroku/linewrap')
  return linewrapOverride(length, stdtermwidth, {
    skipScheme: 'ansi-color'
  })(s).trim()
}

export function getCommandId(argv: string[]) {
  if (argv.length === 1 && ['-v', '--version'].includes(argv[0])) {
    return 'version'
  }
  if (argv.includes('help')) {
    return 'help'
  }
  if (argv[1] && !argv[1].startsWith('-')) {
    return argv.slice(0, 2).join(':')
  } else {
    const firstFlag = argv.findIndex(param => param.startsWith('-'))
    if (firstFlag === -1) {
      return argv.join(':')
    } else {
      return argv.slice(0, firstFlag).join(':')
    }
  }
}
