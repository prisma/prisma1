export function getArgv(value: string): string[] {
  const myRegexp = /([^\s'"]+(['"])([^\2]*?)\2)|[^\s'"]+|(['"])([^\4]*?)\4/gi
  const myString = value
  const myArray = [
    'node',
    'index.js'
  ]
  let match
  do {
    // Each call to exec returns the next regex match as an array
    match = myRegexp.exec(myString)
    if (match !== null) {
      // Index 1 in the array is the captured group if it exists
      // Index 0 is the matched text, which we use if no captured group exists
      myArray.push(match[1] || match[5] || match[0])
    }
  } while (match !== null)

  return myArray
}