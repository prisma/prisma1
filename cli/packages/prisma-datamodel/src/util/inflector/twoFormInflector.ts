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

export interface Rule {
  getPlural(singular: string) : string | null
}

// tslint:disable:max-classes-per-file
export class RegExpRule implements Rule {
  
  private singular: RegExp
  private plural: string

  constructor(singular: RegExp, plural: string) {
    this.singular = singular
    this.plural = plural
  }

  public getPlural(singular: string) : string | null {
    // Important: Do not use g flag, we dont want to replace globally, only the first one
    if(this.singular.test(singular)) {
      const plural = singular.replace(this.singular, this.plural)
      return plural
    } else {
      return null
    }
  }
}

export class CategoryRule implements Rule {
  
  private list: string[]
  private singular: string
  private plural: string

  constructor(list: string[], singular: string, plural: string) {
    this.list = list
    this.singular = singular
    this.plural = plural
  }

  public getPlural(word: string) : string | null {
    const lowerWord = word.toLowerCase()
    for(const suffix of this.list) {
      if(lowerWord.endsWith(suffix)) {
        if(!lowerWord.endsWith(this.singular)) {
          throw new Error("Internal Error")
        }
        return word.substring(0, word.length - this.singular.length) + this.plural
      }
    }
    return null
  }
}


export abstract class TwoFormInflector {
  private rules: Rule[]

  constructor() {
    this.rules = []
  }

  protected getPlural(word: string) {
		for (const rule of this.rules) {
			const result = rule.getPlural(word)
			if (result != null) {
				return result
			}
		}
		return null
  }
  
  protected uncountable(list: string[]) {
    this.rules.push(new CategoryRule(list, "", ""))
  }

  protected irregular(singular: string, plural: string) {
    this.rules.push(new RegExpRule(new RegExp(`${singular.toUpperCase()[0]}${singular.substring(1)}$`), plural.toUpperCase()[0] + plural.substring(1)))
    this.rules.push(new RegExpRule(new RegExp(`${singular.toLowerCase()[0]}${singular.substring(1)}$`), plural.toLowerCase()[0] + plural.substring(1)))
  }

  protected irregularFromList(list: Array<[string, string]>) {
    for(const [a, b] of list) {
      this.irregular(a, b)
    }
  }

  protected rule(singular: string, plural: string) {
    this.rules.push(new RegExpRule(new RegExp(singular, "i"), plural))
  }

  protected ruleFromList(list: Array<[string, string]>) {
    for(const [a, b] of list) {
      this.rule(a, b)
    }
  }
  
  protected categoryRule(list: string[], singular: string, plural: string) {
    this.rules.push(new CategoryRule(list, singular, plural))
  }
}