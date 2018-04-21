export function writeToStdIn(commands: string[], timeout: number = 200) {
  return new Promise(resolve => {
    function loop(combo) {
      if (combo.length > 0) {
        setTimeout(() => {
          process.stdin.write(combo[0])
          loop(combo.slice(1))
        }, timeout)
      } else {
        // process.stdin.end()
        resolve()
      }
    }

    loop(commands)
  })
}

export const DOWN = '\x1B\x5B\x42'
export const UP = '\x1B\x5B\x41'
export const ENTER = '\x0D'
