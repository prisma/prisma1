#!/usr/bin/env node

import * as minimist from 'minimist'
import {Command} from './types'

function main() {
  const argv = minimist(process.argv.slice(2))

  const command = argv._[0] as Command | undefined

}

function onError(e: Error) {
  console.error(e.stack)
  process.exit(1)
}

process.on('unhandledRejection', e => onError(e))
main()


