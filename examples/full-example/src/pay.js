const fetch = require('isomorphic-fetch')
const stripe = require('stripe')(process.env.STRIPE_KEY)
const { fromEvent } = require('graphcool-lib')

const DEBUG = true

// enforce Promise.resolve to be GraphQL compliant
const handler = (cb) => {
  return (event) => {
    return new Promise((resolve, reject) => {
      cb(event).then(resolve).catch(e => {
        resolve({
          error: e.message
        })
      })
    })
  }
}

module.exports = handler(event => {
  const client = fromEvent(event)
  const api = client.api('simple/v1')
  //
  // const userId = event.data.User.node.id
  // const email = event.data.User.node.email
  // const firstName = event.data.User.node.firstName
  // const lastName = event.data.User.node.lastName
  // const stripeToken = event.data.User.node.stripeToken

  const getOrder = (orderId) => {
    return api.request(`query getOrder($orderId: ID!) {
      Order(id: $orderId) {
        id
        stripeToken
        paymentDate
        basket {
          items {
            id
            name
            price
          }
        }
        user {
          id
          email
          firstName
          lastName
          stripeCustomerId
        }
      }
    }`, {orderId})
  }

  const getOrCreateStripeCustomer = (user, stripeToken) => {
    if (user.stripeCustomerId) {
      console.log(`Stripe customer for email ${user.email} already exists`)
      return Promise.resolve(user.stripeCustomerId)
    }
    const {email, firstName, lastName} = user
    console.log(`Creating stripe customer for ${email}`)
    return new Promise((resolve, reject) => {
      stripe.customers.create(
        {
          email,
          description: firstName + ' ' + lastName,
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
  }

  const createStripeCharge = (email, stripeCustomerId, amount, description) => {
    console.log(`Creating stripe charge for ${email}`)
    return new Promise((resolve, reject) => {
      stripeCharge = stripe.charges.create(
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
            console.log(`${charge.amount / 100} ${charge.currency} have been charged`)
            console.log(`Charge description:`)
            console.log(charge.description)
            resolve(stripeCustomerId)
          }
        },
      )
    })
  }

  const updateUserAndOrder = (userId, stripeCustomerId, orderId) => {
    return api.request(`
      mutation update(
        $userId: ID!
        $orderId: ID!
        $stripeCustomerId: String
        $paymentDate: DateTime
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
          paymentDate: $paymentDate
        ) {
          id
        }
      }
    `, { userId, stripeCustomerId, orderId, paymentDate: new Date().toISOString() })
  }

  return getOrder(event.data.orderId)
    .then(({Order}) => {

      if (!Order) {
        throw new Error(`Invalid orderId ${event.data.orderId}`)
      }

      const {user, stripeToken} = Order

      const amount = calcAmount(Order)
      const description = getDescription(Order)

      if (Order.paymentDate) {
        throw new Error(`Order ${Order.id} has already been payed on the ${Order.paymentDate}`)
      }

      return getOrCreateStripeCustomer(user, stripeToken)
        .then(stripeCustomerId => createStripeCharge(user.email, stripeCustomerId, amount, description))
        .then(stripeCustomerId => updateUserAndOrder(user.id, stripeCustomerId, Order.id))
        .then(responseJson => {
          console.log(
            `Successfully updated Graphcool customer: ${JSON.stringify(
              responseJson,
            )}`,
          )

          return {
            data: {
              success: true
            }
          }
        })
    })
    .catch(err => {
      // to not lose the error we log it here
      console.log(err)
      throw err
    })
})

function calcAmount(order) {
  return order.basket.items.reduce((acc, item) => acc + item.price, 0) * 100
}

function getDescription(order) {
  const list = order.basket.items.map(item => {
    return `${item.name}: $${item.price}`
  }).join(', ')

  return `You ordered these items: ${list}. Thanks for your order!`
}
