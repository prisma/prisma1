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
import { TwoFormInflector } from './twoFormInflector'

enum Mode {
  ENGLISH_ANGLICIZED,
  ENGLISH_CLASSICAL,
}

export class English extends TwoFormInflector {
  /**
   * Returns plural form of the given word.
   * <p>
   * For instance:
   * <pre>
   * {@code
   * English.plural("cat") == "cats";
   * }
   * </pre>
   * </p>
   * @param word word in singular form
   * @return plural form of given word
   */
  public static plural(word: string) {
    return English.inflector.getPlural(word)
  }

  /**
   * Returns singular or plural form of the word based on count.
   * <p>
   * For instance:
   * <pre>
   * {@code
   * English.plural("cat", 1) == "cat";
   * English.plural("cat", 2) == "cats";
   * }
   * </pre>
   * </p>
   * @param word word in singular form
   * @param count word count
   * @return form of the word correct for given count
   */
  public static pluralByCount(word: string, count: number) {
    return English.inflector.getPluralByCount(word, count)
  }

  public static setMode(mode: Mode) {
    English.inflector = new English(mode)
  }

  private static CATEGORY_EX_ICES = ['codex', 'murex', 'silex']

  private static CATEGORY_IX_ICES = ['radix', 'helix']
  private static CATEGORY_UM_A = [
    'bacterium',
    'agendum',
    'desideratum',
    'erratum',
    'stratum',
    'datum',
    'ovum',
    'extremum',
    'candelabrum',
  ]
  private static CATEGORY_US_I = [
    'alumnus',
    'alveolus',
    'bacillus',
    'bronchus',
    'locus',
    'nucleus',
    'stimulus',
    'meniscus',
    'thesaurus',
  ]
  private static CATEGORY_ON_A = [
    'criterion',
    'perihelion',
    'aphelion',
    'phenomenon',
    'prolegomenon',
    'noumenon',
    'organon',
    'asyndeton',
    'hyperbaton',
  ]
  private static CATEGORY_A_AE = ['alumna', 'alga', 'vertebra', 'persona']
  private static CATEGORY_O_OS = [
    'albino',
    'archipelago',
    'armadillo',
    'commando',
    'crescendo',
    'fiasco',
    'ditto',
    'dynamo',
    'embryo',
    'ghetto',
    'guano',
    'inferno',
    'jumbo',
    'lumbago',
    'magneto',
    'manifesto',
    'medico',
    'octavo',
    'photo',
    'pro',
    'quarto',
    'canto',
    'lingo',
    'generalissimo',
    'stylo',
    'rhino',
    'casino',
    'auto',
    'macro',
    'zero',
  ]
  private static CATEGORY_O_I = [
    'solo',
    'soprano',
    'basso',
    'alto',
    'contralto',
    'tempo',
    'piano',
    'virtuoso',
  ]
  private static CATEGORY_EN_INA = ['stamen', 'foramen', 'lumen']
  private static CATEGORY_A_ATA = [
    'anathema',
    'enema',
    'oedema',
    'bema',
    'enigma',
    'sarcoma',
    'carcinoma',
    'gumma',
    'schema',
    'charisma',
    'lemma',
    'soma',
    'diploma',
    'lymphoma',
    'stigma',
    'dogma',
    'magma',
    'stoma',
    'drama',
    'melisma',
    'trauma',
    'edema',
    'miasma',
  ]

  private static CATEGORY_IS_IDES = ['iris', 'clitoris']
  private static CATEGORY_US_US = [
    'apparatus',
    'impetus',
    'prospectus',
    'cantus',
    'nexus',
    'sinus',
    'coitus',
    'plexus',
    'status',
    'hiatus',
  ]
  private static CATEGORY_NONE_I = ['afreet', 'afrit', 'efreet']
  private static CATEGORY_NONE_IM = ['cherub', 'goy', 'seraph']
  private static CATEGORY_EX_EXES = [
    'apex',
    'latex',
    'vertex',
    'cortex',
    'pontifex',
    'vortex',
    'index',
    'simplex',
  ]
  private static CATEGORY_IX_IXES = ['appendix']
  private static CATEGORY_S_ES = [
    'acropolis',
    'chaos',
    'lens',
    'aegis',
    'cosmos',
    'mantis',
    'alias',
    'dais',
    'marquis',
    'asbestos',
    'digitalis',
    'metropolis',
    'atlas',
    'epidermis',
    'pathos',
    'bathos',
    'ethos',
    'pelvis',
    'bias',
    'gas',
    'polis',
    'caddis',
    'glottis',
    'rhinoceros',
    'cannabis',
    'glottis',
    'sassafras',
    'canvas',
    'ibis',
    'trellis',
  ]
  private static CATEGORY_MAN_MANS = [
    'human',
    'Alabaman',
    'Bahaman',
    'Burman',
    'German',
    'Hiroshiman',
    'Liman',
    'Nakayaman',
    'Oklahoman',
    'Panaman',
    'Selman',
    'Sonaman',
    'Tacoman',
    'Yakiman',
    'Yokohaman',
    'Yuman',
  ]

  private static inflector = new English()

  constructor(mode: Mode = Mode.ENGLISH_ANGLICIZED) {
    super()
    super.uncountable([
      // 2. Handle words that do not inflect in the plural (such as fish, travois, chassis, nationalities ending
      // endings
      'fish',
      'ois',
      'sheep',
      'deer',
      'pox',
      'itis',

      // words
      'bison',
      'flounder',
      'pliers',
      'bream',
      'gallows',
      'proceedings',
      'breeches',
      'graffiti',
      'rabies',
      'britches',
      'headquarters',
      'salmon',
      'carp',
      'herpes',
      'scissors',
      'chassis',
      'high-jinks',
      'sea-bass',
      'clippers',
      'homework',
      'series',
      'cod',
      'innings',
      'shears',
      'contretemps',
      'jackanapes',
      'species',
      'corps',
      'mackerel',
      'swine',
      'debris',
      'measles',
      'trout',
      'diabetes',
      'mews',
      'tuna',
      'djinn',
      'mumps',
      'whiting',
      'eland',
      'news',
      'wildebeest',
      'elk',
      'pincers',
      'sugar',
    ])

    // 4. Handle standard irregular plurals (mongooses, oxen, etc.)
    super.irregularFromList([
      ['child', 'children'], // classical
      ['ephemeris', 'ephemerides'], // classical
      ['mongoose', 'mongoose'], // anglicized
      ['mythos', 'mythoi'], // classical
      ['soliloquy', 'soliloquies'], // anglicized
      ['trilby', 'trilbys'], // anglicized
      ['genus', 'genera'], // classical
      ['quiz', 'quizzes'],
    ])

    if (mode === Mode.ENGLISH_ANGLICIZED) {
      // Anglicized plural
      super.irregularFromList([
        ['beef', 'beefs'],
        ['brother', 'brothers'],
        ['cos', 'cows'],
        ['genie', 'genies'],
        ['money', 'moneys'],
        ['octopus', 'octopuses'],
        ['opus', 'opuses'],
      ])
    } else if (mode === Mode.ENGLISH_CLASSICAL) {
      // Classical plural
      super.irregularFromList([
        ['beef', 'beeves'],
        ['brother', 'brethren'],
        ['cow', 'kine'],
        ['genie', 'genii'],
        ['money', 'monies'],
        ['octopus', 'octopodes'],
        ['opus', 'opera'],
      ])
    }

    super.categoryRule(English.CATEGORY_MAN_MANS, '', 's')

    // 5. Handle irregular inflections for common suffixes
    super.ruleFromList([
      ['man$', 'men'],
      ['([lm])ouse$', '$1ice'],
      ['tooth$', 'teeth'],
      ['goose$', 'geese'],
      ['foot$', 'feet'],
      ['zoon$', 'zoa'],
      ['([csx])is$', '$1es'],
    ])

    // 6. Handle fully assimilated classical inflections
    super.categoryRule(English.CATEGORY_EX_ICES, 'ex', 'ices')
    super.categoryRule(English.CATEGORY_IX_ICES, 'ix', 'ices')
    super.categoryRule(English.CATEGORY_UM_A, 'um', 'a')
    super.categoryRule(English.CATEGORY_ON_A, 'on', 'a')
    super.categoryRule(English.CATEGORY_A_AE, 'a', 'ae')

    // 7. Handle classical variants of modern inflections
    if (mode === Mode.ENGLISH_CLASSICAL) {
      super.ruleFromList([
        ['trix$', 'trices'],
        ['eau$', 'eaux'],
        ['ieu$', 'ieux'],
        ['(..[iay])nx$', '$1nges'],
      ])
      super.categoryRule(English.CATEGORY_EN_INA, 'en', 'ina')
      super.categoryRule(English.CATEGORY_A_ATA, 'a', 'ata')
      super.categoryRule(English.CATEGORY_IS_IDES, 'is', 'ides')
      super.categoryRule(English.CATEGORY_US_US, '', '')
      super.categoryRule(English.CATEGORY_O_I, 'o', 'i')
      super.categoryRule(English.CATEGORY_NONE_I, '', 'i')
      super.categoryRule(English.CATEGORY_NONE_IM, '', 'im')
      super.categoryRule(English.CATEGORY_EX_EXES, 'ex', 'ices')
      super.categoryRule(English.CATEGORY_IX_IXES, 'ix', 'ices')
    }

    super.categoryRule(English.CATEGORY_US_I, 'us', 'i')

    super.rule('([cs]h|[zx])$', '$1es')
    super.categoryRule(English.CATEGORY_S_ES, '', 'es')
    super.categoryRule(English.CATEGORY_IS_IDES, '', 'es')
    super.categoryRule(English.CATEGORY_US_US, '', 'es')
    super.rule('(us)$', '$1es')
    super.categoryRule(English.CATEGORY_A_ATA, '', 's')

    // The suffixes -ch, -sh, and -ss all take -es in the plural (churches,
    // classes, etc)...
    super.ruleFromList([['([cs])h$', '$1hes'], ['ss$', 'sses']])

    // Certain words ending in -f or -fe take -ves in the plural (lives,
    // wolves, etc)...
    super.ruleFromList([
      ['([aeo]l)f$', '$1ves'],
      ['([^d]ea)f$', '$1ves'],
      ['(ar)f$', '$1ves'],
      ['([nlw]i)fe$', '$1ves'],
    ])

    // Words ending in -y take -ys
    super.ruleFromList([['([aeiou])y$', '$1ys'], ['y$', 'ies']])

    // Some words ending in -o take -os (including does preceded by a vowel)
    super.categoryRule(English.CATEGORY_O_I, 'o', 'os')
    super.categoryRule(English.CATEGORY_O_OS, 'o', 'os')
    super.rule('([aeiou])o$', '$1os')
    // The rest take -oes
    super.rule('o$', 'oes')

    super.rule('ulum', 'ula')

    super.categoryRule(English.CATEGORY_A_ATA, '', 'es')

    super.rule('s$', 'ses')
    // Otherwise, assume that the plural just adds -s
    super.rule('$', 's')
  }

  /**
   * Returns plural form of the given word.
   *
   * @param word word in singular form
   * @return plural form of the word
   */
  public getPlural(word: string) {
    const plural = super.getPlural(word)
    if (plural === word) {
      if (plural.endsWith('s')) {
        return plural + 'es'
      } else {
        return plural + 's'
      }
    }

    return plural
  }

  /**
   * Returns singular or plural form of the word based on count.
   *
   * @param word word in singular form
   * @param count word count
   * @return form of the word correct for given count
   */
  public getPluralByCount(word: string, count: number) {
    if (count === 1) {
      return word
    }
    return this.getPlural(word)
  }
}
