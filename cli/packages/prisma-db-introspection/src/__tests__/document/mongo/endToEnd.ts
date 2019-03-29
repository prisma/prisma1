import { MongoConnector } from '../../../databases/document/mongo/mongoConnector'
import { MongoTestEnvironment } from '../../../test-helpers/mongoTestEnvironment'
import { collections } from '../data/webshop'

const env = new MongoTestEnvironment()

describe('Mongo Model Introspector, end to end', () => {
  beforeAll(async () => await env.connect())
  afterAll(async () => await env.disconnect())
  afterEach(async () => await env.clear())

  it(
    'Webshop',
    async () => {
      await env.createCollections(collections)

      const connector = new MongoConnector(env.getClient())
      const introspection = await connector.introspect(env.schemaName)
      const schema = await introspection.renderToDatamodelString()

      expect(schema).toMatchInlineSnapshot(`
"type items {
  # Type String is currently not supported for id fields.
  _id: String! @id
  keywords: [String] @scalarList(strategy: RELATION)
  price: Float
  rating: Float
  reviews: [itemsReviews]
}

type itemsReviews @embedded {
  rating: Float
  text: String
}

type orders {
  # Type Int is currently not supported for id fields.
  _id: Int! @id
  amount: Float
  customer: users @relation(link: INLINE)
  items: [items] @relation(link: INLINE)
  orderDate: String
}

type users {
  # Type String is currently not supported for id fields.
  _id: String! @id
  firstName: String
  lastName: String
  paymentInfo: [usersPaymentInfo]
  shippingAddress: usersShippingAddress
}

type usersPaymentInfo @embedded {
  accountId: String
  BIC: String
  expires: String
  IBAN: String
  number: String
  type: String
}

type usersShippingAddress @embedded {
  country: String
  number: String
  street: String
}"
`)
      const normalizedSchema = await introspection.renderToNormalizedDatamodelString()

      expect(normalizedSchema).toMatchInlineSnapshot(`
"type Item @db(name: \\"items\\") {
  # Type String is currently not supported for id fields.
  _id: String! @id
  keywords: [String] @scalarList(strategy: RELATION)
  price: Float
  rating: Float
  reviews: [ItemReview]
}

type ItemReview @embedded {
  rating: Float
  text: String
}

type Order @db(name: \\"orders\\") {
  # Type Int is currently not supported for id fields.
  _id: Int! @id
  amount: Float
  customer: User @relation(link: INLINE)
  items: [Item] @relation(link: INLINE)
  orderDate: String
}

type User @db(name: \\"users\\") {
  # Type String is currently not supported for id fields.
  _id: String! @id
  firstName: String
  lastName: String
  paymentInfo: [UserPaymentInfo]
  shippingAddress: UserShippingAddress
}

type UserPaymentInfo @embedded {
  accountId: String
  BIC: String
  expires: String
  IBAN: String
  number: String
  type: String
}

type UserShippingAddress @embedded {
  country: String
  number: String
  street: String
}"
`)
    },
    60000,
  )
})
