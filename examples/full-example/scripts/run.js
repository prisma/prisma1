const pay = require('../code/pay')
const example = require('../example-events/pay.json')

const result = pay(example)

if (result.constructor.name === 'Promise') {
  result.then(printValue)
} else {
  printValue(result)
}

function printValue(value) {
  console.log('Return value')
  console.log(JSON.stringify(value, null, 2))
}
