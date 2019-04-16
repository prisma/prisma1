package com.prisma.integration.deploychecks.airbnb_example

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class AirBnBDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Deploying the AirBnB schema" should "work if there is no data yet" in {
    val (project, _) = setupProject(basicTypesGql)

    deployServer.deploySchemaThatMustSucceed(project, airbnb_setup_data.schema, 3)
  }

  "Removing relations and models from the AirBnB schema that do not have data yet" should "work" in {
    val (project, _) = setupProject(basicTypesGql)

    val updatedProject = deployServer.deploySchema(project, airbnb_setup_data.schema)

    airbnb_setup_data.seedMutations.foreach(mutation => apiServer.query(mutation, updatedProject))

    val simplifiedSchema =
      """type User {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  firstName: String!
        |  lastName: String!
        |  email: String! @unique
        |  password: String!
        |  phone: String!
        |  responseRate: Float
        |  responseTime: Int
        |  isSuperHost: Boolean! @default(value: false)
        |  ownedPlaces: [Place]
        |  location: Location @relation(link: INLINE)
        |  bookings: [Booking]
        |  paymentAccount: [PaymentAccount]
        | # sentMessages: [Message] @relation(name: "SentMessages")
        | # receivedMessages: [Message] @relation(name: "ReceivedMessages")
        | # notifications: [Notification]
        |  profilePicture: Picture @relation(link: INLINE)
        |  hostingExperiences: [Experience]
        |}
        |
        |type Place {
        |  id: ID! @id
        |  name: String
        |  size: PLACE_SIZES
        |  shortDescription: String!
        |  description: String!
        |  slug: String!
        |  maxGuests: Int!
        |  numBedrooms: Int!
        |  numBeds: Int!
        |  numBaths: Int!
        |  # reviews: [Review]
        |  amenities: Amenities!
        |  host: User! @relation(link: INLINE)
        |  pricing: Pricing! @relation(link: INLINE)
        |  location: Location! @relation(link: INLINE)
        |  views: Views! @relation(link: INLINE)
        |  guestRequirements: GuestRequirements @relation(link: INLINE)
        |  policies: Policies @relation(link: INLINE)
        |  houseRules: HouseRules @relation(link: INLINE)
        |  bookings: [Booking]
        |  pictures: [Picture]
        |  popularity: Int!
        |}
        |
        |type Pricing {
        |  id: ID! @id
        |  place: Place!
        |  monthlyDiscount: Int
        |  weeklyDiscount: Int
        |  perNight: Int!
        |  smartPricing: Boolean! @default(value: false)
        |  basePrice: Int!
        |  averageWeekly: Int!
        |  averageMonthly: Int!
        |  cleaningFee: Int
        |  securityDeposit: Int
        |  extraGuests: Int
        |  weekendPricing: Int
        |  currency: CURRENCY
        |}
        |
        |type GuestRequirements {
        |  id: ID! @id
        |  govIssuedId: Boolean! @default(value: false)
        |  recommendationsFromOtherHosts: Boolean! @default(value: false)
        |  guestTripInformation: Boolean! @default(value: false)
        |  place: Place!
        |}
        |
        |type Policies {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  checkInStartTime: Float!
        |  checkInEndTime: Float!
        |  checkoutTime: Float!
        |  place: Place!
        |}
        |
        |type HouseRules {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  suitableForChildren: Boolean
        |  suitableForInfants: Boolean
        |  petsAllowed: Boolean
        |  smokingAllowed: Boolean
        |  partiesAndEventsAllowed: Boolean
        |  additionalRules: String
        |}
        |
        |type Views {
        |  id: ID! @id
        |  lastWeek: Int!
        |  place: Place!
        |}
        |
        |type Location {
        |  id: ID! @id
        |  lat: Float!
        |  lng: Float!
        |  neighbourHood: Neighbourhood
        |  user: User
        |  place: Place
        |  address: String
        |  directions: String
        |  experience: Experience
        |  restaurant: Restaurant
        |}
        |
        |type Neighbourhood {
        |  id: ID! @id
        |  locations: [Location]
        |  name: String!
        |  slug: String!
        |  homePreview: Picture @relation(link: INLINE)
        |  city: City! @relation(link: INLINE)
        |  featured: Boolean!
        |  popularity: Int!
        |}
        |
        |type City {
        |  id: ID! @id
        |  name: String!
        |  neighbourhoods: [Neighbourhood]
        |}
        |
        |type Picture {
        |  id: ID! @id
        |  url: String!
        |}
        |
        |type Experience {
        |  id: ID! @id
        |  category: ExperienceCategory @relation(link: INLINE)
        |  title: String!
        |  host: User! @relation(link: INLINE)
        |  location: Location! @relation(link: INLINE)
        |  pricePerPerson: Int!
        | # reviews: [Review]
        |  preview: Picture! @relation(link: INLINE)
        |  popularity: Int!
        |}
        |
        |type ExperienceCategory {
        |  id: ID! @id
        |  mainColor: String! @default(value: "#123456")
        |  name: String!
        |  experience: Experience
        |}
        |
        |type Amenities {
        |  id: ID! @id
        |  place: Place! @relation(link: INLINE)
        |  elevator: Boolean! @default(value: false)
        |  petsAllowed: Boolean! @default(value: false)
        |  internet: Boolean! @default(value: false)
        |  kitchen: Boolean! @default(value: false)
        |  wirelessInternet: Boolean! @default(value: false)
        |  familyKidFriendly: Boolean! @default(value: false)
        |  freeParkingOnPremises: Boolean! @default(value: false)
        |  hotTub: Boolean! @default(value: false)
        |  pool: Boolean! @default(value: false)
        |  smokingAllowed: Boolean! @default(value: false)
        |  wheelchairAccessible: Boolean! @default(value: false)
        |  breakfast: Boolean! @default(value: false)
        |  cableTv: Boolean! @default(value: false)
        |  suitableForEvents: Boolean! @default(value: false)
        |  dryer: Boolean! @default(value: false)
        |  washer: Boolean! @default(value: false)
        |  indoorFireplace: Boolean! @default(value: false)
        |  tv: Boolean! @default(value: false)
        |  heating: Boolean! @default(value: false)
        |  hangers: Boolean! @default(value: false)
        |  iron: Boolean! @default(value: false)
        |  hairDryer: Boolean! @default(value: false)
        |  doorman: Boolean! @default(value: false)
        |  paidParkingOffPremises: Boolean! @default(value: false)
        |  freeParkingOnStreet: Boolean! @default(value: false)
        |  gym: Boolean! @default(value: false)
        |  airConditioning: Boolean! @default(value: false)
        |  shampoo: Boolean! @default(value: false)
        |  essentials: Boolean! @default(value: false)
        |  laptopFriendlyWorkspace: Boolean! @default(value: false)
        |  privateEntrance: Boolean! @default(value: false)
        |  buzzerWirelessIntercom: Boolean! @default(value: false)
        |  babyBath: Boolean! @default(value: false)
        |  babyMonitor: Boolean! @default(value: false)
        |  babysitterRecommendations: Boolean! @default(value: false)
        |  bathtub: Boolean! @default(value: false)
        |  changingTable: Boolean! @default(value: false)
        |  childrensBooksAndToys: Boolean! @default(value: false)
        |  childrensDinnerware: Boolean! @default(value: false)
        |  crib: Boolean! @default(value: false)
        |}
        |
        |#type Review {
        |#  id: ID! @id
        |#  createdAt: DateTime! @createdAt
        |#  text: String!
        |#  stars: Int!
        |#  accuracy: Int!
        |#  location: Int!
        |#  checkIn: Int!
        |#  value: Int!
        |#  cleanliness: Int!
        |#  communication: Int!
        |#  place: Place!
        |#  experience: Experience
        |#}
        |
        |type Booking {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  bookee: User! @relation(link: INLINE)
        |  place: Place! @relation(link: INLINE)
        |  startDate: DateTime!
        |  endDate: DateTime!
        |#  payment: Payment!
        |}
        |
        |#type Payment {
        |#  id: ID! @id
        |#  createdAt: DateTime! @createdAt
        |#  serviceFee: Float!
        |#  placePrice: Float!
        |#  totalPrice: Float!
        |#  booking: Booking!
        |#  paymentMethod: PaymentAccount!
        |#}
        |
        |type PaymentAccount {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  type: PAYMENT_PROVIDER
        |  user: User! @relation(link: INLINE)
        |#  payments: [Payment]
        |  paypal: PaypalInformation @relation(link: INLINE)
        |  creditcard: CreditCardInformation @relation(link: INLINE)
        |}
        |
        |type PaypalInformation {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  email: String!
        |  paymentAccount: PaymentAccount!
        |}
        |
        |type CreditCardInformation {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  cardNumber: String!
        |  expiresOnMonth: Int!
        |  expiresOnYear: Int!
        |  securityCode: String!
        |  firstName: String!
        |  lastName: String!
        |  postalCode: String!
        |  country: String!
        |  paymentAccount: PaymentAccount
        |}
        |
        |#type Message {
        |#  id: ID! @id
        |#  createdAt: DateTime! @createdAt
        |#  from: User! @relation(name: "SentMessages")
        |#  to: User! @relation(name: "ReceivedMessages")
        |#  deliveredAt: DateTime!
        |#  readAt: DateTime!
        |#}
        |
        |#type Notification {
        |#  id: ID! @id
        |#  createdAt: DateTime! @createdAt
        |#  type: NOTIFICATION_TYPE
        |#  user: User!
        |#  link: String!
        |#  readDate: DateTime!
        |#}
        |
        |type Restaurant {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  title: String!
        |  avgPricePerPerson: Int!
        |  pictures: [Picture]
        |  location: Location! @relation(link: INLINE)
        |  isCurated: Boolean! @default(value: true)
        |  slug: String!
        |  popularity: Int!
        |}
        |
        |
        |enum CURRENCY {
        |  CAD
        |  CHF
        |  EUR
        |  JPY
        |  USD
        |  ZAR
        |}
        |
        |enum PLACE_SIZES {
        |  ENTIRE_HOUSE
        |  ENTIRE_APARTMENT
        |  ENTIRE_EARTH_HOUSE
        |  ENTIRE_CABIN
        |  ENTIRE_VILLA
        |  ENTIRE_PLACE
        |  ENTIRE_BOAT
        |  PRIVATE_ROOM
        |}
        |
        |enum PAYMENT_PROVIDER {
        |  PAYPAL
        |  CREDIT_CARD
        |}
        |
        |enum NOTIFICATION_TYPE {
        |  OFFER
        |  INSTANT_BOOK
        |  RESPONSIVENESS
        |  NEW_AMENITIES
        |  HOUSE_RULES
        |}
        |""".stripMargin

    deployServer.deploySchemaThatMustSucceed(updatedProject, simplifiedSchema, 4)
  }

  "Removing relations and models from the AirBnB schema that already have data" should "throw a warning" in {

    val (project, _) = setupProject(basicTypesGql)

    val updatedProject = deployServer.deploySchema(project, airbnb_setup_data.schema)

    airbnb_setup_data.seedMutations.foreach(mutation => apiServer.query(mutation, updatedProject))

    val simplifiedSchema =
      """type User {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  firstName: String!
        |  lastName: String!
        |  email: String! @unique
        |  password: String!
        |  phone: String!
        |  responseRate: Float
        |  responseTime: Int
        |  isSuperHost: Boolean! @default(value: false)
        |  ownedPlaces: [Place]
        |  location: Location @relation(link: INLINE)
        |  bookings: [Booking]
        |  paymentAccount: [PaymentAccount]
        |  sentMessages: [Message] @relation(name: "SentMessages")
        |  receivedMessages: [Message] @relation(name: "ReceivedMessages")
        |  notifications: [Notification]
        | # profilePicture: Picture @relation(link: INLINE)
        |  hostingExperiences: [Experience]
        |}
        |
        |type Place {
        |  id: ID! @id
        |  name: String
        |  size: PLACE_SIZES
        |  shortDescription: String!
        |  description: String!
        |  slug: String!
        |  maxGuests: Int!
        |  numBedrooms: Int!
        |  numBeds: Int!
        |  numBaths: Int!
        |  reviews: [Review]
        |  amenities: Amenities!
        |  host: User! @relation(link: INLINE)
        |  pricing: Pricing! @relation(link: INLINE)
        |  location: Location! @relation(link: INLINE)
        |  views: Views! @relation(link: INLINE)
        |  guestRequirements: GuestRequirements @relation(link: INLINE)
        |  policies: Policies @relation(link: INLINE)
        |  houseRules: HouseRules @relation(link: INLINE)
        |  bookings: [Booking]
        | # pictures: [Picture]
        |  popularity: Int!
        |}
        |
        |type Pricing {
        |  id: ID! @id
        |  place: Place!
        |  monthlyDiscount: Int
        |  weeklyDiscount: Int
        |  perNight: Int!
        |  smartPricing: Boolean! @default(value: false)
        |  basePrice: Int!
        |  averageWeekly: Int!
        |  averageMonthly: Int!
        |  cleaningFee: Int
        |  securityDeposit: Int
        |  extraGuests: Int
        |  weekendPricing: Int
        |  currency: CURRENCY
        |}
        |
        |type GuestRequirements {
        |  id: ID! @id
        |  govIssuedId: Boolean! @default(value: false)
        |  recommendationsFromOtherHosts: Boolean! @default(value: false)
        |  guestTripInformation: Boolean! @default(value: false)
        |  place: Place!
        |}
        |
        |type Policies {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  checkInStartTime: Float!
        |  checkInEndTime: Float!
        |  checkoutTime: Float!
        |  place: Place!
        |}
        |
        |type HouseRules {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  suitableForChildren: Boolean
        |  suitableForInfants: Boolean
        |  petsAllowed: Boolean
        |  smokingAllowed: Boolean
        |  partiesAndEventsAllowed: Boolean
        |  additionalRules: String
        |}
        |
        |type Views {
        |  id: ID! @id
        |  lastWeek: Int!
        |  place: Place!
        |}
        |
        |type Location {
        |  id: ID! @id
        |  lat: Float!
        |  lng: Float!
        |#  neighbourHood: Neighbourhood
        |  user: User
        |  place: Place
        |  address: String
        |  directions: String
        |  experience: Experience
        |  restaurant: Restaurant
        |}
        |
        |#type Neighbourhood {
        |#  id: ID! @id
        |#  locations: [Location]
        |#  name: String!
        |#  slug: String!
        |#  homePreview: Picture @relation(link: INLINE)
        |#  city: City! @relation(link: INLINE)
        |#  featured: Boolean!
        |#  popularity: Int!
        |#}
        |
        |type City {
        |  id: ID! @id
        |  name: String!
        | # neighbourhoods: [Neighbourhood]
        |}
        |
        |#type Picture {
        |#  url: String!
        |#}
        |
        |type Experience {
        |  id: ID! @id
        |  category: ExperienceCategory @relation(link: INLINE)
        |  title: String!
        |  host: User! @relation(link: INLINE)
        |  location: Location! @relation(link: INLINE)
        |  pricePerPerson: Int!
        |  reviews: [Review]
        |#  preview: Picture! @relation(link: INLINE)
        |  popularity: Int!
        |}
        |
        |type ExperienceCategory {
        |  id: ID! @id
        |  mainColor: String! @default(value: "#123456")
        |  name: String!
        |  experience: Experience
        |}
        |
        |type Amenities {
        |  id: ID! @id
        |  place: Place! @relation(link: INLINE)
        |
        |  elevator: Boolean! @default(value: false)
        |  petsAllowed: Boolean! @default(value: false)
        |  internet: Boolean! @default(value: false)
        |  kitchen: Boolean! @default(value: false)
        |  wirelessInternet: Boolean! @default(value: false)
        |  familyKidFriendly: Boolean! @default(value: false)
        |  freeParkingOnPremises: Boolean! @default(value: false)
        |  hotTub: Boolean! @default(value: false)
        |  pool: Boolean! @default(value: false)
        |  smokingAllowed: Boolean! @default(value: false)
        |  wheelchairAccessible: Boolean! @default(value: false)
        |  breakfast: Boolean! @default(value: false)
        |  cableTv: Boolean! @default(value: false)
        |  suitableForEvents: Boolean! @default(value: false)
        |  dryer: Boolean! @default(value: false)
        |  washer: Boolean! @default(value: false)
        |  indoorFireplace: Boolean! @default(value: false)
        |  tv: Boolean! @default(value: false)
        |  heating: Boolean! @default(value: false)
        |  hangers: Boolean! @default(value: false)
        |  iron: Boolean! @default(value: false)
        |  hairDryer: Boolean! @default(value: false)
        |  doorman: Boolean! @default(value: false)
        |  paidParkingOffPremises: Boolean! @default(value: false)
        |  freeParkingOnStreet: Boolean! @default(value: false)
        |  gym: Boolean! @default(value: false)
        |  airConditioning: Boolean! @default(value: false)
        |  shampoo: Boolean! @default(value: false)
        |  essentials: Boolean! @default(value: false)
        |  laptopFriendlyWorkspace: Boolean! @default(value: false)
        |  privateEntrance: Boolean! @default(value: false)
        |  buzzerWirelessIntercom: Boolean! @default(value: false)
        |  babyBath: Boolean! @default(value: false)
        |  babyMonitor: Boolean! @default(value: false)
        |  babysitterRecommendations: Boolean! @default(value: false)
        |  bathtub: Boolean! @default(value: false)
        |  changingTable: Boolean! @default(value: false)
        |  childrensBooksAndToys: Boolean! @default(value: false)
        |  childrensDinnerware: Boolean! @default(value: false)
        |  crib: Boolean! @default(value: false)
        |}
        |
        |type Review {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  text: String!
        |  stars: Int!
        |  accuracy: Int!
        |  location: Int!
        |  checkIn: Int!
        |  value: Int!
        |  cleanliness: Int!
        |  communication: Int!
        |  place: Place! @relation(link: INLINE)
        |  experience: Experience @relation(link: INLINE)
        |}
        |
        |type Booking {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  bookee: User! @relation(link: INLINE)
        |  place: Place! @relation(link: INLINE)
        |  startDate: DateTime!
        |  endDate: DateTime!
        |  payment: Payment!
        |}
        |
        |type Payment {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  serviceFee: Float!
        |  placePrice: Float!
        |  totalPrice: Float!
        |  booking: Booking! @relation(link: INLINE)
        |  paymentMethod: PaymentAccount! @relation(link: INLINE)
        |}
        |
        |type PaymentAccount {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  type: PAYMENT_PROVIDER
        |  user: User!
        |  payments: [Payment]
        |  paypal: PaypalInformation @relation(link: INLINE)
        |  creditcard: CreditCardInformation @relation(link: INLINE)
        |}
        |
        |type PaypalInformation {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  email: String!
        |  paymentAccount: PaymentAccount!
        |}
        |
        |type CreditCardInformation {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  cardNumber: String!
        |  expiresOnMonth: Int!
        |  expiresOnYear: Int!
        |  securityCode: String!
        |  firstName: String!
        |  lastName: String!
        |  postalCode: String!
        |  country: String!
        |  paymentAccount: PaymentAccount
        |}
        |
        |type Message {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  from: User! @relation(name: "SentMessages" link: INLINE)
        |  to: User! @relation(name: "ReceivedMessages" link: INLINE)
        |  deliveredAt: DateTime!
        |  readAt: DateTime!
        |}
        |
        |type Notification {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  type: NOTIFICATION_TYPE
        |  user: User! @relation(link: INLINE)
        |  link: String!
        |  readDate: DateTime!
        |}
        |
        |type Restaurant {
        |  id: ID! @id
        |  createdAt: DateTime! @createdAt
        |  title: String!
        |  avgPricePerPerson: Int!
        |#  pictures: [Picture]
        |  location: Location! @relation(link: INLINE)
        |  isCurated: Boolean! @default(value: true)
        |  slug: String!
        |  popularity: Int!
        |}
        |
        |
        |enum CURRENCY {
        |  CAD
        |  CHF
        |  EUR
        |  JPY
        |  USD
        |  ZAR
        |}
        |
        |enum PLACE_SIZES {
        |  ENTIRE_HOUSE
        |  ENTIRE_APARTMENT
        |  ENTIRE_EARTH_HOUSE
        |  ENTIRE_CABIN
        |  ENTIRE_VILLA
        |  ENTIRE_PLACE
        |  ENTIRE_BOAT
        |  PRIVATE_ROOM
        |}
        |
        |enum PAYMENT_PROVIDER {
        |  PAYPAL
        |  CREDIT_CARD
        |}
        |
        |enum NOTIFICATION_TYPE {
        |  OFFER
        |  INSTANT_BOOK
        |  RESPONSIVENESS
        |  NEW_AMENITIES
        |  HOUSE_RULES
        |}
        |""".stripMargin

    deployServer.deploySchemaThatMustWarn(updatedProject, simplifiedSchema)

  }

}
