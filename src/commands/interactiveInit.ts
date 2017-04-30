import {SystemEnvironment} from '../types'
import {howDoYouWantToGetStarted} from '../utils/constants'
const term = require( 'terminal-kit' ).terminal
import * as chalk from 'chalk'
import figures = require('figures')

export default async (env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  const options = [
    `${figures.pointer} Instagram starter kit`,
    `  Blank project`,
    `  Schema from disk`,
    `  One more option`,
    `  Hmm and waht else?`,
  ]

  out.write(howDoYouWantToGetStarted(options))

  term.grabInput()
  term.up(options.length)
  term.hideCursor()
  let currentIndex = 0

  term.on( 'key' , function( name ) {
    currentIndex = handleKeyEvent(name, currentIndex, options)

    if ( name === 'CTRL_C' ) {
      term.hideCursor()
      process.exit()
    }

  })

}

function handleKeyEvent(name: string, currentIndex: number, options: string[]): number {

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
