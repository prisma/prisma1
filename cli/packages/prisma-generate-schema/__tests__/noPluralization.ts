import { plural } from '../src/util/util'

const exampleWordList = [
  ['News', 'Newses'],
  ['Homework', 'Homeworks'],
  ['Scissors', 'Scissorses']
]

test('Must work correctly with types that can not be pluralized', () => {
  for (const [singular, pluralForm] of exampleWordList) {
    const res = plural(singular)
    expect(res).toEqual(pluralForm)
  }
})