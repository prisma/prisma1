import { SystemEnvironment } from '../types'
import {howDoYouWantToGetStarted, instagramExampleSchemaUrl, sampleSchemaURL} from '../utils/constants'
const term = require('terminal-kit').terminal
import * as chalk from 'chalk'
import figures = require('figures')
import * as _ from 'lodash'
import initCommand from './init'
const debug = require('debug')('graphcool')

const INSTAGRAM_STARTER = 0
const BLANK_PROJECT = 1

type CheckAuth = () => Promise<void>

interface Props {
  checkAuth: CheckAuth
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {

  const {out} = env

  const schemaFiles = env.resolver.schemaFiles('.')

  const options = [
    [`${chalk.bold('Quickstart')}`, `Sets up a full-stack Instagram example to explore how things work.`, ''],
    [`${chalk.bold('New blank project')}`, `Creates a new Graphcool project from scratch.`, ''],
    ...schemaFiles.map(f => [`${chalk.bold('From local schema ./' + f)}`, 'Creates a new Graphcool project based on the local schema.', '']),
  ]

  term.saveCursor()
  out.write(howDoYouWantToGetStarted())

  term.grabInput()
  term.hideCursor()

  let currentIndex = INSTAGRAM_STARTER // 0

  render(options, currentIndex)

  await new Promise(resolve => {
    term.on('key', async (name: string) => {
      currentIndex = await handleKeyEvent(name, currentIndex, options, props.checkAuth, env, resolve)
    })
  })
}

async function handleKeyEvent(name: string, currentIndex: number, options: string[][], checkAuth: CheckAuth, env: SystemEnvironment, callback: () => void): Promise<number> {

  switch (name) {
    case 'DOWN': {
      currentIndex = (currentIndex + 1) % options.length
      rerender(options, currentIndex)
      break
    }
    case 'UP': {
      currentIndex = (currentIndex + options.length - 1) % options.length
      rerender(options, currentIndex)
      break
    }
    case 'ENTER': {
      await handleSelect(currentIndex, checkAuth, env)
      callback()
      break
    }
    case 'CTRL_C': {
      term.restoreCursor()
      term.eraseDisplayBelow()
      term.hideCursor(false)
      env.out.write('\n')
      process.exit()
    }
    default: {
      break
    }
  }

  return currentIndex
}

async function handleSelect(selectedIndex: number, checkAuth: CheckAuth, env: SystemEnvironment): Promise<void> {
  term.restoreCursor()
  term.eraseDisplayBelow()
  term.hideCursor(false)
  env.out.write('\n')

  if (selectedIndex === INSTAGRAM_STARTER || selectedIndex === BLANK_PROJECT) {
    term.grabInput(false)

    await checkAuth()
  }

  switch (selectedIndex) {
    case INSTAGRAM_STARTER: {
      const remoteSchemaUrl = instagramExampleSchemaUrl
      await initCommand({remoteSchemaUrl}, env)
      break
    }
    case BLANK_PROJECT: {
      const remoteSchemaUrl = sampleSchemaURL
      await initCommand({remoteSchemaUrl}, env)
      break
    }
    default: {
      term.grabInput(false)
      const schemaFiles = env.resolver.schemaFiles('.')
      const previousOptions = 2
      if (selectedIndex > schemaFiles.length + previousOptions) {
        break
      }
      const projectFileIndex = selectedIndex - previousOptions
      const localSchemaFile = schemaFiles[projectFileIndex]
      await initCommand({localSchemaFile}, env)
      break
    }
  }
}

function rerender(options: string[][], currentIndex: number): void {
  clear(options)
  render(options, currentIndex)
}

function clear(options: string[][]) {
  const lineCount = _.flatten(options).length - 1
  term.up(lineCount)
  term.left(10000)
  term.eraseDisplayBelow()
}

function render(options: string[][], currentIndex: number) {
  const lines = _.chain(options)
    .map((ls, optionIndex) => ls.map((l, lineIndex) => (lineIndex === 0 && optionIndex === currentIndex) ? `  ${chalk.blue(figures.pointer)} ${l}` : `    ${l}`))
    .flatten()
    .join('\n')

  term(lines, currentIndex)
}
