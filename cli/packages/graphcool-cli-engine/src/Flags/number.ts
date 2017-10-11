import { Flag } from './index'

export default function number(options: Flag<string> = {}): Flag<number> {
  return {
    ...options,
    parse: (input: string = options.defaultValue) => parseInt(input, 10),
  }
}
