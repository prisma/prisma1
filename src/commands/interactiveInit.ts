import { SystemEnvironment } from '../types'
import { howDoYouWantToGetStarted, instagramExampleSchemaUrl } from '../utils/constants'
const term = require('terminal-kit').terminal
import * as chalk from 'chalk'
import figures = require('figures')
import initCommand from './init'
import { writeExampleSchemaFile } from '../utils/file'
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
    `  ${chalk.blue(figures.pointer)} Quickstart (Instagram tutorial - recommended for beginners)`,
    `    New project from scratch`,
    ...schemaFiles.map(f => `    From local schema ./${f}`),
  ]

  term.saveCursor()
  out.write(howDoYouWantToGetStarted(options))

  term.grabInput()
  term.up(options.length)
  term.hideCursor()
  let currentIndex = INSTAGRAM_STARTER // 0

  await new Promise(resolve => {
    term.on('key', async (name: string) => {
      currentIndex = await handleKeyEvent(name, currentIndex, options, props.checkAuth, env, resolve)
    })
  })
}

async function handleKeyEvent(name: string, currentIndex: number, options: string[], checkAuth: CheckAuth, env: SystemEnvironment, callback: () => void): Promise<number> {

  switch (name) {
    case 'DOWN': {
      if (currentIndex < options.length - 1) {
        options[currentIndex] = replaceFirstCharacters(options[currentIndex], 8, '   ')
        options[currentIndex + 1] = replaceFirstCharacters(options[currentIndex + 1], 3, `  ${chalk.blue(figures.pointer)}`)
        overwriteNextLines([options[currentIndex], options[currentIndex + 1]])
        currentIndex++
      }
      break
    }
    case 'UP': {
      if (currentIndex > 0) {
        options[currentIndex] = replaceFirstCharacters(options[currentIndex], 8, '   ')
        options[currentIndex - 1] = replaceFirstCharacters(options[currentIndex - 1], 3, `  ${chalk.blue(figures.pointer)}`)
        overwritePreviousLines([options[currentIndex], options[currentIndex - 1]])
        currentIndex--
      }
      break
    }
    case 'ENTER': {
      await handleSelect(currentIndex, options, checkAuth, env)
      callback()
      break
    }
    case 'CTRL_C': {
      term.restoreCursor()
      term.eraseDisplayBelow()
      term.hideCursor(false)
      term.eraseDisplayBelow()
      env.out.write('\n')
      process.exit()
    }
    default: {
      break
    }
  }

  return currentIndex
}

async function handleSelect(selectedIndex: number, options: string[], checkAuth: CheckAuth, env: SystemEnvironment): Promise<void> {
  term.restoreCursor()
  term.up(options.length + 3) // 3 is depending on the help text
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
      const name = 'Instagram'
      await initCommand({remoteSchemaUrl, name}, env)
      break
    }
    case BLANK_PROJECT: {
      const localSchemaFile = writeExampleSchemaFile(env.resolver)
      await initCommand({localSchemaFile}, env)
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

function overwriteNextLines(lines: string[]): void {
  lines.forEach((line, index) => {
    term.eraseLineAfter()
    term.defaultColor(line)
    term.left(10000)
    if (index < lines.length - 1) {
      term.down()
    }
  })
}

function overwritePreviousLines(lines: string[]): void {
  lines.forEach((line, index) => {
    term.eraseLineBefore()
    term.defaultColor(line)
    term.left(10000)
    if (index < lines.length - 1) {
      term.up()
    }
  })
}

function replaceFirstCharacters(str, n, insert) {
  const tmp = str.substring(n, str.length)
  return insert + tmp
}
