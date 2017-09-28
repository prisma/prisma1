import { Flag } from './index'

export default function string(options: Flag<string> = {}): Flag<string> {
  return {
    parse: (input: string) => input,
    ...options,
  }
}
