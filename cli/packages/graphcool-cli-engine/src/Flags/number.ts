import { Flag } from './index'

export default function number(options: Flag<string> = {}): Flag<number> {
  return {
    ...options,
    parse: (input: string) => parseInt(input, 10),
  }
}
