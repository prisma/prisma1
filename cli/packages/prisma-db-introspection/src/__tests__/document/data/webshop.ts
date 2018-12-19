
export const users = [{
  _id: "test1@prisma.com",
  firstName: "Test",
  lastName: "Test",
  paymentInfo: [{
    type: "creditCard",
    expires: "12/18",
    number: "12345678"
  }],
  shippingAddress: {
    street: "Teststreet",
    country: "AT",
    number: "10"
  },
}, {
  _id: "test2@prisma.com",
  firstName: "Test",
  lastName: "Test",
  paymentInfo: [{
    type: "SEPA",
    IBAN: "00000000012345",
    BIC: "WASDJK12"
  }],
  shippingAddress: {
    street: "Teststreet2",
    country: "DE",
    number: "10"
  },
},  {
  _id: "test3@prisma.com",
  firstName: "Test",
  lastName: "Test",
  paymentInfo: [{
    type: "paypal",
    accountId: "12345"
  }, {
    type: "creditCard",
    expires: "12/18",
    number: "8674535"
  }],
  shippingAddress: {
    street: "Teststreet2",
    country: "DE",
    number: "10"
  },
}]

export const items = [{
  _id: "Toaster",
  price: 5.99,
  rating: 4.3, 
  reviews: [{
    rating: 0,
    text: "What a bad toaster."
  }, {
    rating: 5,
    text: "What a good toaster."
  }, {
    rating: 2.5,
    text: "What an ok toaster."
  }],
  keywords: ["kitchen", "toaster", "toast"]
}, {
  _id: "Fridge",
  price: 25,
  rating: 2, 
  reviews: [{
    rating: 0
  }, {
    rating: 5,
    text: "What a good fridge."
  }],
  keywords: ["kitchen", "fridge", "yoghurt"]
}, {
  _id: "Bike",
  price: 299,
  rating: 5, 
  reviews: [],
  keywords: ["road"]
}, {
  _id: "TV",
  price: 10,
  rating: 0, 
  reviews: [{ rating: 0 }, { rating: 0 }],
  keywords: ["hollywood"]
}]

export const orders = [{
  _id: 0,
  customer: "test1@prisma.com",
  orderDate: "2018-12-21",
  amount: 255,
  // Array of Relation on purpuse. 
  items: ["Bike", "TV", "Fridge"]
}, {
  _id: 1,
  customer: "test3@prisma.com",
  orderDate: "2018-12-22",
  amount: 300,
  items: ["TV", "TV", "TV"]
}, {
  _id: 2,
  customer: "test2@prisma.com",
  orderDate: "2018-12-22",
  amount: 3000.5,
  // FK error on purpose
  items: ["Car"]
}]

export const collections = {
  User: users,
  Order: orders, 
  Item: items
}

export const schemaString = `type Item {
  # Type String is currently not supported for id fields.
  _id: String! @id
  keywords: [String!]!
  price: Float
  rating: Float
  reviews: [ItemReviews!]!
}

type ItemReviews @embedded {
  rating: Float
  text: String
}

type Order {
  # Type Int is currently not supported for id fields.
  _id: Int! @id
  amount: Float
  customer: User @relation(link: INLINE)
  items: [Item!]! @relation(link: INLINE)
  orderDate: String
}

type User {
  # Type String is currently not supported for id fields.
  _id: String! @id
  firstName: String
  lastName: String
  paymentInfo: [UserPaymentInfo!]!
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
}`
