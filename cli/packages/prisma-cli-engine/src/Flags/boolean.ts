import { Flag } from './index'

export default function boolean(options: Flag<any> = {}): Flag<boolean> {
  return {
    ...options,
    parse: undefined,
  }
}
