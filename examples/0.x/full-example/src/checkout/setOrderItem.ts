import * as fetch from 'isomorphic-fetch'
import { fromEvent, FunctionEvent } from 'graphcool-lib'
import { GraphQLClient } from 'graphql-request'

interface EventData {
  orderId: string
  productId: string
  amount: number
}

interface User {
  id: string
  email: string
  stripeCustomerId: string
}

interface Item {
  id: string
  amount: number
}

interface Order {
  id: string
  description: string
  user: User | null
  items: [Item]
  orderStatus: string
}

interface Product {
  id: string
}


export default async (event: FunctionEvent<EventData>) => {
  console.log(event)

  try {
    if (event.data.amount < 0) {
      event.data.amount = 0
    }

    // check if request is authenticated by a user
    if (!event.context.auth || !event.context.auth.nodeId || event.context.auth.typeName !== 'User') {
      return { error: 'Insufficient Permissions' }
    }

    const authenticatedUserId = event.context.auth.nodeId

    const client = fromEvent(event)
    const api = client.api('simple/v1')

    const { productId, orderId, amount } = event.data

    const { order, product } = await getOrderAndProduct(api, orderId, productId)
      
    if (!order || !order.user || !order.items || !product) {
      throw new Error(`Error when querying the order and product!`)
    }

    console.log(order)

    if (order.orderStatus === 'PAID') {
      return {
        error: {
          message: 'Order has already been paid',
          orderId: `${order.id}`
        }
      }
    }

    // check if authenticated user owns specified order
    if (order.user.id !== authenticatedUserId) {
      return { error: 'Insufficient Permissions' }
    }
    

    const { user, items } = order


    if (amount === 0) {
      if (items.length === 0) {
        return { data: { amount } }
      } else {
        const oldItem = items[0]
        await deleteItem(api, oldItem.id)
        return { data: { amount } }
      }
    } else {
      if (items.length === 0) {
        const { item } = await createItem(api, orderId, productId, amount)
        return { data: { amount: item.amount, itemId: item.id }}
      } else {
        const oldItem = items[0]
        const { item } = await updateItem(api, oldItem.id, amount)
        return { data: { amount: item.amount, itemId: item.id }}
      }
    }
  } catch(e) {
    console.log(e)

    return { error: 'An unexpected error when adding items to the order!' }
  }
}

async function getOrderAndProduct(api: GraphQLClient, orderId: string, productId: string): Promise<{ order: Order, product: Product }> {
  return api.request<{ order: Order, product: Product }>(`
  query getOrderAndProduct($orderId: ID!, $productId: ID!) {
    order: Order(id: $orderId) {
      id
      orderStatus
      items(filter: {
        product: {
          id: $productId
        }
      }) {
        id
        amount
      }
      user {
        id
      }
    }
    product: Product(id: $productId) {
      id
    }
  }`, {orderId, productId})
}

const createItem = async (api: GraphQLClient, orderId: string, productId: string, amount: number): Promise<{ item: Item }> => {
  return api.request<{ item: Item }>(`
    mutation (
      $orderId: ID!
      $productId: ID!
      $amount: Int!
    ) {
      item: createItem(
        orderId: $orderId
        productId: $productId
        amount: $amount
      ) {
        id
        amount
      }
    }
  `, { orderId, productId, amount })
}

const updateItem = async (api: GraphQLClient, orderId: string, amount: number): Promise<{ item: Item }> => {
  return api.request<{ item: Item }>(`
    mutation (
      $orderId: ID!
      $amount: Int!
    ) {
      item: updateItem(
        id: $orderId
        amount: $amount
      ) {
        id
        amount
      }
    }
  `, { orderId, amount })
}

const deleteItem = async (api: GraphQLClient, orderId: string): Promise<any> => {
  return api.request(`
    mutation (
      $orderId: ID!
    ) {
      item: deleteItem(
        id: $orderId
      ) {
        id
        amount
      }
    }
  `, { orderId })
}