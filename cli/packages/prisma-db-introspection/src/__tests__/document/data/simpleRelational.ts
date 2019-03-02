import { IGQLType, SdlExpect, TypeIdentifiers } from 'prisma-datamodel'

/*
 * users and items are a small datamodel to test relations.
 */
export const users = [
  {
    _id: 'user1@prisma.com',
    firstName: 'Charlotte',
    orders: [
      {
        count: 5,
        item: 'Fridge',
      },
      {
        count: 1,
        item: 'Espresso',
      },
    ],
  },
  {
    _id: 'user2@prisma.com',
    firstName: 'Dolores',
    orders: [],
  },
  {
    _id: 'user3@prisma.com',
    firstName: 'Humbert',
    orders: [
      {
        count: 2,
        item: 'Laptop',
      },
    ],
  },
]

export const items = [
  {
    _id: 'Fridge',
    cost: 200,
  },
  {
    _id: 'Laptop',
    cost: 2500,
  },
  {
    _id: 'Espresso',
    cost: 1,
  },
]

export const schemaString = `type Item {
  # Type String is currently not supported for id fields.
  _id: String! @id
  cost: Int
}

type User {
  # Type String is currently not supported for id fields.
  _id: String! @id
  firstName: String
  orders: [UserOrders]
}

type UserOrders @embedded {
  count: Int
  item: Item @relation(link: INLINE)
}`

export function assertUserItemModel(allTypes: IGQLType[]) {
  const userType = SdlExpect.type(allTypes, 'User', false, false)
  const ordersType = SdlExpect.type(allTypes, 'UserOrders', false, true)
  const itemType = SdlExpect.type(allTypes, 'Item', false, false)

  expect(userType.fields).toHaveLength(3)
  SdlExpect.field(userType, 'orders', false, true, ordersType)
  SdlExpect.field(userType, '_id', true, false, TypeIdentifiers.string, true)
  SdlExpect.field(userType, 'firstName', false, false, TypeIdentifiers.string)

  expect(ordersType.fields).toHaveLength(2)
  SdlExpect.field(ordersType, 'item', false, false, itemType)
  SdlExpect.field(ordersType, 'count', false, false, TypeIdentifiers.integer)

  expect(itemType.fields).toHaveLength(2)
  SdlExpect.field(itemType, '_id', true, false, TypeIdentifiers.string, true)
  SdlExpect.field(itemType, 'cost', false, false, TypeIdentifiers.integer)
}

// Start: 13:38
