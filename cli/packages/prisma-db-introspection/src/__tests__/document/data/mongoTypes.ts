import * as BSON from 'bson'

// Client doc: https://mongodb.github.io/node-mongodb-native/api-bson-generated/bson.html
// Database doc: https://docs.mongodb.com/manual/reference/bson-types/
export const scalars = [{
  _id: 0,
  double: new BSON.Double(0.5),
  string: 'string',
  object: { test: 'test' },
  array: [1, 2, 3],
  binary: new BSON.Binary(new Buffer(5)),
  objectId: new BSON.ObjectID(),
  boolean: true,
  date: new Date(),
  null: null,
  regex: new BSON.BSONRegExp('\\w', 'i'),
  javascript: new BSON.Code('alert("hello")'),
  int32: new BSON.Int32(10),
  timestamp: new BSON.Timestamp(10, 10),
  int64: new BSON.Long(10, 10),
  decimal128: new BSON.Decimal128(new Buffer(4))
}]

export const schemaString = `type scalars {
  # Type Int is currently not supported for id fields.
  _id: Int! @id
  array: [Int]
  # Field type not supported: Binary
  # binary: <Unknown>
  boolean: Boolean
  date: DateTime
  # Field type not supported: Decimal128
  # decimal128: <Unknown>
  double: Float
  int32: Int
  int64: Int
  # Field type not supported: Code
  # javascript: <Unknown>
  # Field type not supported: null
  # null: <Unknown>
  object: scalarsObject
  objectId: ID
  # Field type not supported: RegExp
  # regex: <Unknown>
  string: String
  # Field type not supported: Timestamp
  # timestamp: <Unknown>
}

type scalarsObject @embedded {
  test: String
}`