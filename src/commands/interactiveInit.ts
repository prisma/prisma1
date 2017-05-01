import {SystemEnvironment} from '../types'
import {howDoYouWantToGetStarted, instagramExampleSchemaUrl} from '../utils/constants'
const term = require( 'terminal-kit' ).terminal
import figures = require('figures')
import initCommand from './init'
import {writeExampleSchemaFile} from '../utils/file'
const debug = require('debug')('graphcool')

const INSTAGRAM_STARTER = 0
const BLANK_PROJECT = 1

export default async (env: SystemEnvironment): Promise<void> => {

  const {out} = env

  const options = [
    `${figures.pointer} Instagram starter kit`,
    `  Blank project`,
  ]

  term.saveCursor()
  out.write(howDoYouWantToGetStarted(options))

  term.grabInput()
  term.up(options.length)
  term.hideCursor()
  let currentIndex = INSTAGRAM_STARTER // 0

  term.on('key', (name: string) => {
    currentIndex = handleKeyEvent(name, currentIndex, options, env)
  })

}

function handleSelect(selectedIndex: number, env: SystemEnvironment) {
  term.restoreCursor()
  term.eraseDisplayBelow()
  term.hideCursor(false)

  switch (selectedIndex) {
    case INSTAGRAM_STARTER: {
      term.grabInput(false)
      const remoteSchemaUrl = instagramExampleSchemaUrl
      const name = 'Instagram'
      initCommand({remoteSchemaUrl, name}, env)
      break
    }
    case BLANK_PROJECT: {
      term.grabInput(false)
      const localSchemaFile = writeExampleSchemaFile(env.resolver)
      initCommand({localSchemaFile}, env)
      break
    }
    default: {
      break
    }
  }
}

function handleKeyEvent(name: string, currentIndex: number, options: string[], env: SystemEnvironment): number {

  switch (name) {
    case 'DOWN': {
      if (currentIndex < options.length - 1) {
        options[currentIndex] = replaceFirstCharacters(options[currentIndex], 1, ' ')
        options[currentIndex+1] = replaceFirstCharacters(options[currentIndex+1], 1, `${figures.pointer}`)
        overwriteNextLines([options[currentIndex], options[currentIndex+1]])
        currentIndex++
      }
      break
    }
    case 'UP': {
      if (currentIndex > 0) {
        options[currentIndex] = replaceFirstCharacters(options[currentIndex], 1, ' ')
        options[currentIndex-1] = replaceFirstCharacters(options[currentIndex-1], 1, `${figures.pointer}`)
        overwritePreviousLines([options[currentIndex], options[currentIndex-1]])
        currentIndex--
      }
      break
    }
    case 'ENTER': {
      handleSelect(currentIndex, env)
      break
    }
    case 'CTRL_C': {
      term.hideCursor(false)
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
    term.white(line)
    term.left(10000)
    if (index < lines.length - 1) {
      term.down()
    }
  })
}

function overwritePreviousLines(lines: string[]): void {
  lines.forEach((line, index) => {
    term.eraseLineBefore()
    term.white(line)
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
