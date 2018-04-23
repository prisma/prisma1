import { Validator } from './Validator'

const types = `
      type Post {
        id: ID!
      }
      enum AASD {
        A
      }
    `
const validator = new Validator(types)
