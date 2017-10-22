import * as fetch from 'isomorphic-fetch'
import { fromEvent, FunctionEvent } from 'graphcool-lib'
import { GraphQLClient } from 'graphql-request'
import * as Stripe from 'stripe'

const stripe = new Stripe(process.env.STRIPE_KEY!)

interface EventData {
  stripeToken: string | null
  orderId: string
}

interface Item {
  id: string
  amount: number
  order: Order
  product: Product
}

interface Product {
  id: string
  name: string
  price: number
}

interface User {
  id: string
  email: string
  name: string
  stripeCustomerId: string
}

interface Order {
  id: string
  description: string
  orderStatus: string
  items: [Item]
  user: User
}

export default async (event: FunctionEvent<EventData>) => {
  console.log(event)

  try {
    if (!process.env['STRIPE_KEY']) {
      console.log('Please provide a valid Stripe key!')
      return { error: 'Not configured correctly.' }
    }

    // check if request is authenticated by a user
    if (!event.context.auth || !event.context.auth.nodeId || event.context.auth.typeName !== 'User') {
      return { error: 'Insufficient Permissions' }
    }

    const authenticatedUserId = event.context.auth.nodeId

    const client = fromEvent(event)
    const api = client.api('simple/v1')

    const { stripeToken, orderId } = event.data

    const order = await getOrder(api, orderId)
      .then(data => data.Order)
      
    if (!order || !order.user ) {
      throw new Error(`Error when querying order`)
    }

    const user = order.user

    // check if paid
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

    let stripeCustomerId = user.stripeCustomerId
    if (!user.stripeCustomerId) {
      if (!stripeToken) {
        return { error: 'You need to provide a valid Stripe token!' }
      } else {
        stripeCustomerId = await createStripeCustomer(user, stripeToken)
      }
    } else {
      console.log(`Stripe customer for email ${user.email} already exists`)
    }

    const total = calulateTotal(order)
    const description = getDescription(order)

    await createStripeCharge(user.email, stripeCustomerId, total, description)
    await updateUserAndOrder(api, user.id, stripeCustomerId, orderId)

    return {
      data: {
        success: true
      }
    }
  } catch(e) {
    console.log(e)

    return { error: 'An unexpected error occured during checkout.' }
  }
}

async function getOrder(api: GraphQLClient, orderId: string): Promise<{ Order: Order }> {
  return api.request<{ Order: Order }>(`
  query getOrder($orderId: ID!) {
    Order(id: $orderId) {
      id
      orderStatus
      items {
        id
        amount
        product {
          id
          price
        }
      }
      user {
        id
        email
        stripeCustomerId
      }
    }
  }`, {orderId})
}

function calulateTotal(order: Order): number {
  if (order.items.length === 0) {
    return 0
  }

  return order.items.reduce((acc, item) => acc + item.amount*item.product.price, 0) * 100
}

function getDescription(order: Order): string {
  if (order.items.length === 0) {
    return 'Empty order.'
  }

  const list = order.items.map(item => {
    return `${item.amount}x $${item.product.name}`
  }).join(', ')

  return `You ordered these items: ${list}. Thanks for your order!`
}

const createStripeCustomer = async (user: User, stripeToken: string): Promise<any> => {
  const { email, name } = user
  console.log(`Creating stripe customer for ${email}`)

  const data = new Promise((resolve, reject) => {

    stripe.customers.create(
      {
        email,
        description: name,
        source: stripeToken,
      },
      (err, customer) => {
        if (err) {
          console.log(
            `Error creating stripe customer: ${JSON.stringify(err)}`,
          )
          reject(err)
        } else {
          console.log(`Successfully created stripe customer: ${customer.id}`)
          resolve(customer.id)
        }
      },
    )
  })

  return data
}

const createStripeCharge = async (email: string, stripeCustomerId: string, amount: number, description: string): Promise<any> => {
  return new Promise((resolve, reject) => {
    stripe.charges.create(
      {
        amount,
        description,
        currency: 'usd',
        customer: stripeCustomerId,
      },
      (err, charge) => {
        if (err) {
          console.log(`Error creating Stripe charge: ${JSON.stringify(err)}`)
          reject(err)
        } else {
          console.log(`Successfully created Stripe charge: ${charge.id}`)
          resolve(charge.id)
        }
      },
    )
  })
}

const updateUserAndOrder = async (api: GraphQLClient, userId: string, stripeCustomerId: string, orderId: string): Promise<any> => {

  return api.request(`
    mutation update(
      $userId: ID!
      $orderId: ID!
      $stripeCustomerId: String
    ) {
      updateUser(
        id: $userId,
        stripeCustomerId: $stripeCustomerId,
      ) {
        id
        email
      }
      updateOrder(
        id: $orderId,
        orderStatus: PAID
      ) {
        id
      }
    }
  `, { userId, stripeCustomerId, orderId })
}