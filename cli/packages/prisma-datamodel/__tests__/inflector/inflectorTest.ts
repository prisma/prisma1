/*
 * Copyright 2011 Atteo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
import { English } from '../../src/util/inflector/english'
import GQLAssert from '../../src/util/gqlAssert'

/**
 * Unit tests ported from evo-inflector
 * https://github.com/atteo/evo-inflector
 */

const inflector = new English()

// Wiktionary test is skipped.
const exampleWordList = [
  ['alga', 'algae'],
  ['nova', 'novas'],
  ['dogma', 'dogmas'],
  ['Woman', 'Women'],
  ['church', 'churches'],
  ['quick_chateau', 'quick_chateaus'],
  ['codex', 'codices'],
  ['index', 'indexes'],
  ['NightWolf', 'NightWolves'],
  ['Milieu', 'Milieus'],
  ['basis', 'bases'],
  ['iris', 'irises'],
  ['phalanx', 'phalanxes'],
  ['tempo', 'tempos'],
  ['foot', 'feet'],
  ['series', 'serieses'],
  ['WorldAtlas', 'WorldAtlases'],
  ['wish', 'wishes'],
  ['Bacterium', 'Bacteria'],
  ['medium', 'mediums'],
  ['Genus', 'Genera'],
  ['stimulus', 'stimuli'],
  ['opus', 'opuses'],
  ['status', 'statuses'],
  ['Box', 'Boxes'],
  ['ferry', 'ferries'],
  ['protozoon', 'protozoa'],
  ['cherub', 'cherubs'],
  ['human', 'humans'],
  ['sugar', 'sugars'],
  ['virus', 'viruses'],
  ['gastrostomy', 'gastrostomies'],
  ['baculum', 'bacula'],
  ['pancreas', 'pancreases'],
  ['news', 'newses'],
  ['scissors', 'scissorses'],
]

test('Inflector should correctly inflect our test word list', () => {
  for (const [singular, plural] of exampleWordList) {
    const res = inflector.getPlural(singular)
    expect(res).toEqual(plural)
  }
})

test('Inflector should correctly inflect when a count is given', () => {
  expect('cat').toEqual(inflector.getPluralByCount('cat', 1))
  expect('cats').toEqual(inflector.getPluralByCount('cat', 2))

  expect('demoness').toEqual(inflector.getPluralByCount('demoness', 1))
  expect('demonesses').toEqual(inflector.getPluralByCount('demoness', 2))
})

test('Inflector should correctly implement static medthos', () => {
  expect('sulfimides').toEqual(English.plural('sulfimide'))
  expect('semifluids').toEqual(English.pluralByCount('semifluid', 2))
})
