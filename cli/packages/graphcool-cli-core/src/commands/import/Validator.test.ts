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
        id: ID!
        title: String
      }
    `
      const validator = new Validator(types)
      expect(validator.validateNode({ _typeName: 'Post', id: 'asd' })).toBe(
        true,
      )
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
  })

  describe('validateListNode', () => {
    test('List', () => {
      const types = `
      type Post {
        id: ID!
        tags: [String!]!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateListNode({
          _typeName: 'Post',
          id: '25',
          tags: [1, 2, 3],
        }),
      ).toThrow()
      expect(() =>
        validator.validateListNode({ _typeName: 'Post', id: '25', tags: '' }),
      ).toThrow()
      expect(
        validator.validateListNode({
          _typeName: 'Post',
          id: '25',
          tags: ['a', 'b', 'c'],
        }),
      ).toBe(true)
    })
  })

  describe('validateRelationTuple', () => {
    test('relations', () => {
      const types = `
      type Post {
        id: ID!
        self: Post! @relation(name: "Some Name")
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateRelationTuple([
          {
            _typeName: 'Post',
            fieldName: 'self2',
            id: '23',
          },
          {
            _typeName: 'Post',
            fieldName: 'self',
            id: '25',
          },
        ]),
      ).toThrow()
      expect(() =>
        validator.validateRelationTuple([
          {
            _typeName: 'Post',
            fieldName: 'self2',
            id: '23',
          },
        ] as any),
      ).toThrow()
      expect(() =>
        validator.validateRelationTuple([
          {
            _typeName: 'Post2',
            fieldName: 'self',
            id: '23',
          },
          {
            _typeName: 'Post',
            fieldName: 'self',
            id: '25',
          },
        ] as any),
      ).toThrow()
      expect(() =>
        validator.validateRelationTuple([
          {
            _typeName: 'Post2',
            fieldName: 'self',
          },
          {
            _typeName: 'Post',
            fieldName: 'self',
            id: '25',
          },
        ] as any),
      ).toThrow()
      expect(
        validator.validateRelationTuple([
          {
            _typeName: 'Post',
            fieldName: 'self',
            id: '23',
          },
          {
            _typeName: 'Post',
            fieldName: 'self',
            id: '25',
          },
        ]),
      ).toBe(true)
    })
  })

  describe('validateImportData', () => {
    test('nodes', () => {
      const types = `
      type Post {
        id: ID!
        string: String!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateImportData({
          valueType: 'nodes',
          values: [
            {
              _typeName: 'Post',
              id: 'a',
              string: '',
            },
            {
              _typeName: 'Post',
              id: 'b',
            },
          ],
        }),
      ).toThrow()
      expect(
        validator.validateImportData({
          valueType: 'nodes',
          values: [
            {
              _typeName: 'Post',
              id: 'a',
              string: '',
            },
            {
              _typeName: 'Post',
              id: 'b',
              string: '',
            },
            {
              _typeName: 'Post',
              id: 'c',
              string: '',
            },
            {
              _typeName: 'Post',
              id: 'd',
              string: '',
            },
            {
              _typeName: 'Post',
              id: 'e',
              string: '',
            },
          ],
        }),
      ).toBe(true)
    })
    test('relations', () => {
      const types = `
      type Post {
        id: ID!
        self: Post! @relation(name: "RelationName")
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateImportData({
          valueType: 'relations',
          values: [
            [
              {
                _typeName: 'Post',
                id: 'a',
                fieldName: 'self',
              },
              {
                _typeName: 'Post',
                id: 'b',
                fieldName: 'self2',
              },
            ],
          ],
        }),
      ).toThrow()
      expect(
        validator.validateImportData({
          valueType: 'relations',
          values: [
            [
              {
                _typeName: 'Post',
                id: 'a',
                fieldName: 'self',
              },
              {
                _typeName: 'Post',
                id: 'b',
                fieldName: 'self',
              },
            ],
          ],
        }),
      ).toBe(true)
    })
    test('lists', () => {
      const types = `
      type Post {
        id: ID!
        nonScalarButRequired: Int!
        listValue: [String!]!
      }
    `
      const validator = new Validator(types)
      expect(() =>
        validator.validateImportData({
          valueType: 'lists',
          values: [
            {
              _typeName: 'Post',
              id: 'a',
              nonScalarButRequired: 5,
              listValue: ['a', 'b', 'c'],
            },
            {
              _typeName: 'Post',
              id: 'b',
              nonScalarButRequired: 5,
              listValue: ['a', 'b', 'c'],
            },
          ],
        }),
      ).toThrow()
      expect(
        validator.validateImportData({
          valueType: 'lists',
          values: [
            {
              _typeName: 'Post',
              id: 'a',
              listValue: ['a', 'b', 'c'],
            },
            {
              _typeName: 'Post',
              id: 'b',
              listValue: ['a', 'b', 'c'],
            },
          ],
        }),
      ).toBe(true)
    })
  })
})
