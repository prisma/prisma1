import { collections } from '../data/webshop'
import { MockDocumentDataSource } from '../../../test-helpers/mockDataSource'

describe('Document relation inferring, end to end examples', () => {
  it('Webshop', async () => {
    const mockDataSource = new MockDocumentDataSource(collections)
    const result = await mockDataSource.introspect('default')

    expect(await result.renderToDatamodelString()).toMatchInlineSnapshot(`
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
  })
})
