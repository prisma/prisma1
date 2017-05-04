import { SystemEnvironment } from '../types'
import { howDoYouWantToGetStarted, instagramExampleSchemaUrl } from '../utils/constants'
const term = require('terminal-kit').terminal
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
    `  ${figures.pointer} Instagram starter kit`,
    `    Blank project`,
    ...schemaFiles.map(f => `    From ./${f}`),
  ]

  term.saveCursor()
  out.write(howDoYouWantToGetStarted(options))

  term.grabInput()
  term.up(options.length + 1) // +1 because of the new line at the end
  term.hideCursor()
  let currentIndex = INSTAGRAM_STARTER // 0

  term.on('key', (name: string) => {
    currentIndex = handleKeyEvent(name, currentIndex, options, props.checkAuth, env)
  })

}

async function handleSelect(selectedIndex: number, checkAuth: CheckAuth, env: SystemEnvironment) {
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
      const name = 'Instagram'
      initCommand({remoteSchemaUrl, name}, env)
      break
    }
    case BLANK_PROJECT: {
      const localSchemaFile = writeExampleSchemaFile(env.resolver)
      initCommand({localSchemaFile}, env)
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
      initCommand({localSchemaFile}, env)
      break
    }
  }
}

function handleKeyEvent(name: string, currentIndex: number, options: string[], checkAuth: CheckAuth, env: SystemEnvironment): number {

  switch (name) {
    case 'DOWN': {
      if (currentIndex < options.length - 1) {
        options[currentIndex] = replaceFirstCharacters(options[currentIndex], 3, '   ')
        options[currentIndex + 1] = replaceFirstCharacters(options[currentIndex + 1], 3, `  ${figures.pointer}`)
        overwriteNextLines([options[currentIndex], options[currentIndex + 1]])
        currentIndex++
      }
      break
    }
    case 'UP': {
      if (currentIndex > 0) {
        options[currentIndex] = replaceFirstCharacters(options[currentIndex], 3, '   ')
        options[currentIndex - 1] = replaceFirstCharacters(options[currentIndex - 1], 3, `  ${figures.pointer}`)
        overwritePreviousLines([options[currentIndex], options[currentIndex - 1]])
        currentIndex--
      }
      break
    }
    case 'ENTER': {
      handleSelect(currentIndex, checkAuth, env)
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
