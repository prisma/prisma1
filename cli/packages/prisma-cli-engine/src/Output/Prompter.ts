import * as ansi from 'ansi-escapes'
import { Output } from './index'
import { PromptMaskError } from '../errors/PromptMaskError'

export interface PromptOptions {
  name?: string
  prompt?: string
  mask?: boolean
  hide?: boolean
}

export default class Prompter {
  out: Output

  constructor(out: Output) {
    this.out = out
  }

  prompt(name: string, options: PromptOptions = {}): Promise<string> {
    options = options || {}
    options.name = name
    options.prompt = name
      ? this.out.color.dim(`${name}: `)
      : this.out.color.dim('> ')
    const isTTY = process.env.TERM !== 'dumb' && (process.stdin as any).isTTY
    return this.out.action.pause(() => {
      if (options.mask || options.hide) {
        if (!isTTY) {
          return Promise.reject(
            new PromptMaskError(
              `CLI needs to prompt for ${options.name ||
                options.prompt ||
                'unknown'} but stdin is not a tty.`,
            ),
          )
        }

        return this.promptMasked(options)
      } else {
        return new Promise(resolve => {
          process.stdin.setEncoding('utf8')
          this.out.stderr.write(options.prompt || '>')
          process.stdin.resume()
          process.stdin.once('data', data => {
            process.stdin.pause()
            data = data.trim()
            if (data === '') {
              resolve(this.prompt(name, { name }))
            } else {
              resolve(data)
            }
          })
        })
      }
    })
  }

  promptMasked(options: PromptOptions): Promise<string> {
    return new Promise((resolve, reject) => {
      const { stdin, stderr } = process
      let input = ''
      stdin.setEncoding('utf8')
      stderr.write(ansi.eraseLine)
      stderr.write(ansi.cursorLeft)
      this.out.stderr.write(options.prompt || '>')
      stdin.resume()
      ;(stdin as any).setRawMode(true)

      function stop() {
        if (!options.hide) {
          stderr.write(
            ansi.cursorHide +
              ansi.cursorLeft +
              options.prompt +
              input.replace(/./g, '*') +
              '\n' +
              ansi.cursorShow,
          )
        } else {
          stderr.write('\n')
        }
        stdin.removeListener('data', fn)
        ;(stdin as any).setRawMode(false)
        stdin.pause()
      }

      function enter() {
        if (input.length === 0) {
          return
        }
        stop()
        resolve(input)
      }

      function ctrlc() {
        reject(new Error('SIGINT'))
        stop()
      }

      function backspace() {
        if (input.length === 0) {
          return
        }
        input = input.substr(0, input.length - 1)
        stderr.write(ansi.cursorBackward(1))
        stderr.write(ansi.eraseEndLine)
      }

      function newchar(c) {
        input += c
        stderr.write(options.hide ? '*'.repeat(c.length) : c)
      }

      function fn(c) {
        switch (c) {
          case '\u0004': // Ctrl-d
          case '\r':
          case '\n':
            return enter()
          case '\u0003': // Ctrl-c
            return ctrlc()
          default:
            // backspace
            if (c.charCodeAt(0) === 127) {
              return backspace()
            } else {
              return newchar(c)
            }
        }
      }
      stdin.on('data', fn)
    })
  }
}
