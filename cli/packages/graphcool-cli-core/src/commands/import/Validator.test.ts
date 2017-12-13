import { Validator } from './Validator'
import * as fs from 'fs-extra'

describe('Validator', () => {
  describe('valideNode', () => {
    test('throws when _typeName missing', () => {
      const types = `
      type Post {
        title: String
      }
    `
      const validator = new Validator(types)
      expect(() => validator.validateNode({})).toThrow()
    })

    test('throws for unkown _typeName', () => {
      const types = `
      type Post {
        title: String
      }
    `
      const validator = new Validator(types)
      expect(() => validator.validateNode({ _typeName: 'Post2' })).toThrow()
    })

    test('ignores non-required field', () => {
      const types = `
      type Post {
        title: String
      }
    `
      const validator = new Validator(types)
      expect(validator.validateNode({ _typeName: 'Post' })).toBe(true)
    })

    test('throws non-existing required field', () => {
      const types = `
      type Post {
        title: String!
      }
    `
      const validator = new Validator(types)
      expect(() => validator.validateNode({ _typeName: 'Post' })).toThrow()
    })

    test('ID', () => {
      const types = `
      type Post {
        id: ID!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: 25 }),
      ).toThrow()
      expect(validator.validateNode({ _typeName: 'Post', id: '25' })).toBe(true)
    })

    test('String', () => {
      const types = `
      type Post {
        id: ID!
        title: String!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', title: true }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          title: 'Some Title',
        }),
      ).toBe(true)
    })

    test('Boolean', () => {
      const types = `
      type Post {
        id: ID!
        bool: Boolean!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', bool: 0 }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          bool: false,
        }),
      ).toBe(true)
    })

    test('DateTime', () => {
      const types = `
      type Post {
        id: ID!
        date: DateTime!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', date: '' }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          date: '2017-12-13T14:09:25.012Z',
        }),
      ).toBe(true)
    })

    test('Int', () => {
      const types = `
      type Post {
        id: ID! int: Int!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', int: 10.5 }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          int: 10,
        }),
      ).toBe(true)
    })

    test('Float', () => {
      const types = `
      type Post {
        id: ID!
        float: Float!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', float: 'hi' }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          float: 20.2,
        }),
      ).toBe(true)
    })

    test('Enum', () => {
      const types = `
      type Post {
        id: ID!
        enum: Tree
      }

      enum Tree {
        Giant_sequoia
        Coconut
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', enum: '' }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          enum: 'Coconut',
        }),
      ).toBe(true)
    })

    test('List', () => {
      const types = `
      type Post {
        id: ID!
        tags: [String!]!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          tags: [1, 2, 3],
        }),
      ).toThrow()
      expect(() =>
        validator.validateNode({ _typeName: 'Post', id: '25', tags: '' }),
      ).toThrow()
      expect(
        validator.validateNode({
          _typeName: 'Post',
          id: '25',
          tags: ['a', 'b', 'c'],
        }),
      ).toBe(true)
    })
  })
})
