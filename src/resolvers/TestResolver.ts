import {Resolver} from '../types'

export default class TestResolver implements Resolver {

  storage: { [key: string] : string }

  constructor(storage: { [key: string] : string }) {
    this.storage = storage
  }

  read(path: string): string {
    return this.storage[path]
  }

  write(path: string, value: string) {
    this.storage[path] = value
  }

  delete(path: string) {
    delete this.storage[path]
  }

}

