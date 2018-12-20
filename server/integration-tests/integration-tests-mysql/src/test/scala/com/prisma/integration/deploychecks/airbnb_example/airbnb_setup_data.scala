package com.prisma.integration.deploychecks.airbnb_example

object airbnb_setup_data {

  val schema =
    """type User {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  updatedAt: DateTime!
      |  firstName: String!
      |  lastName: String!
      |  email: String! @unique
      |  password: String!
      |  phone: String!
      |  responseRate: Float
      |  responseTime: Int
      |
      |  isSuperHost: Boolean! @default(value: "false")
      |  ownedPlaces: [Place]
      |  location: Location
      |  bookings: [Booking]
      |  paymentAccount: [PaymentAccount]
      |  sentMessages: [Message] @relation(name: "SentMessages")
      |  receivedMessages: [Message] @relation(name: "ReceivedMessages")
      |  notifications: [Notification]
      |  profilePicture: Picture
      |  hostingExperiences: [Experience]
      |}
      |
      |type Place {
      |  id: ID! @unique
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
      |  host: User!
      |  pricing: Pricing!
      |  location: Location!
      |  views: Views!
      |  guestRequirements: GuestRequirements
      |  policies: Policies
      |  houseRules: HouseRules
      |  bookings: [Booking]
      |  pictures: [Picture]
      |  popularity: Int!
      |}
      |
      |type Pricing {
      |  id: ID! @unique
      |  place: Place!
      |  monthlyDiscount: Int
      |  weeklyDiscount: Int
      |  perNight: Int!
      |  smartPricing: Boolean! @default(value: "false")
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
      |  id: ID! @unique
      |  govIssuedId: Boolean! @default(value: "false")
      |  recommendationsFromOtherHosts: Boolean! @default(value: "false")
      |  guestTripInformation: Boolean! @default(value: "false")
      |  place: Place!
      |}
      |
      |type Policies {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  updatedAt: DateTime!
      |  checkInStartTime: Float!
      |  checkInEndTime: Float!
      |  checkoutTime: Float!
      |  place: Place!
      |}
      |
      |type HouseRules {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  updatedAt: DateTime!
      |  suitableForChildren: Boolean
      |  suitableForInfants: Boolean
      |  petsAllowed: Boolean
      |  smokingAllowed: Boolean
      |  partiesAndEventsAllowed: Boolean
      |  additionalRules: String
      |}
      |
      |type Views {
      |  id: ID! @unique
      |  lastWeek: Int!
      |  place: Place!
      |}
      |
      |type Location {
      |  id: ID! @unique
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
      |  id: ID! @unique
      |  locations: [Location]
      |  name: String!
      |  slug: String!
      |  homePreview: Picture
      |  city: City!
      |  featured: Boolean!
      |  popularity: Int!
      |}
      |
      |type City {
      |  id: ID! @unique
      |  name: String!
      |  neighbourhoods: [Neighbourhood]
      |}
      |
      |type Picture {
      |  url: String!
      |}
      |
      |type Experience {
      |  id: ID! @unique
      |  category: ExperienceCategory
      |  title: String!
      |  host: User!
      |  location: Location!
      |  pricePerPerson: Int!
      |  reviews: [Review]
      |  preview: Picture!
      |  popularity: Int!
      |}
      |
      |type ExperienceCategory {
      |  id: ID! @unique
      |  mainColor: String! @default(value: "#123456")
      |  name: String!
      |  experience: Experience
      |}
      |
      |type Amenities {
      |  id: ID! @unique
      |  place: Place!
      |  elevator: Boolean! @default(value: "false")
      |  petsAllowed: Boolean! @default(value: "false")
      |  internet: Boolean! @default(value: "false")
      |  kitchen: Boolean! @default(value: "false")
      |  wirelessInternet: Boolean! @default(value: "false")
      |  familyKidFriendly: Boolean! @default(value: "false")
      |  freeParkingOnPremises: Boolean! @default(value: "false")
      |  hotTub: Boolean! @default(value: "false")
      |  pool: Boolean! @default(value: "false")
      |  smokingAllowed: Boolean! @default(value: "false")
      |  wheelchairAccessible: Boolean! @default(value: "false")
      |  breakfast: Boolean! @default(value: "false")
      |  cableTv: Boolean! @default(value: "false")
      |  suitableForEvents: Boolean! @default(value: "false")
      |  dryer: Boolean! @default(value: "false")
      |  washer: Boolean! @default(value: "false")
      |  indoorFireplace: Boolean! @default(value: "false")
      |  tv: Boolean! @default(value: "false")
      |  heating: Boolean! @default(value: "false")
      |  hangers: Boolean! @default(value: "false")
      |  iron: Boolean! @default(value: "false")
      |  hairDryer: Boolean! @default(value: "false")
      |  doorman: Boolean! @default(value: "false")
      |  paidParkingOffPremises: Boolean! @default(value: "false")
      |  freeParkingOnStreet: Boolean! @default(value: "false")
      |  gym: Boolean! @default(value: "false")
      |  airConditioning: Boolean! @default(value: "false")
      |  shampoo: Boolean! @default(value: "false")
      |  essentials: Boolean! @default(value: "false")
      |  laptopFriendlyWorkspace: Boolean! @default(value: "false")
      |  privateEntrance: Boolean! @default(value: "false")
      |  buzzerWirelessIntercom: Boolean! @default(value: "false")
      |  babyBath: Boolean! @default(value: "false")
      |  babyMonitor: Boolean! @default(value: "false")
      |  babysitterRecommendations: Boolean! @default(value: "false")
      |  bathtub: Boolean! @default(value: "false")
      |  changingTable: Boolean! @default(value: "false")
      |  childrensBooksAndToys: Boolean! @default(value: "false")
      |  childrensDinnerware: Boolean! @default(value: "false")
      |  crib: Boolean! @default(value: "false")
      |}
      |
      |type Review {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  text: String!
      |  stars: Int!
      |  accuracy: Int!
      |  location: Int!
      |  checkIn: Int!
      |  value: Int!
      |  cleanliness: Int!
      |  communication: Int!
      |  place: Place!
      |  experience: Experience
      |}
      |
      |type Booking {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  bookee: User!
      |  place: Place!
      |  startDate: DateTime!
      |  endDate: DateTime!
      |  payment: Payment!
      |}
      |
      |type Payment {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  serviceFee: Float!
      |  placePrice: Float!
      |  totalPrice: Float!
      |  booking: Booking!
      |  paymentMethod: PaymentAccount!
      |}
      |
      |type PaymentAccount {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  type: PAYMENT_PROVIDER
      |  user: User!
      |  payments: [Payment]
      |  paypal: PaypalInformation
      |  creditcard: CreditCardInformation
      |}
      |
      |type PaypalInformation {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  email: String!
      |  paymentAccount: PaymentAccount!
      |}
      |
      |type CreditCardInformation {
      |  id: ID! @unique
      |  createdAt: DateTime!
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
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  from: User! @relation(name: "SentMessages")
      |  to: User! @relation(name: "ReceivedMessages")
      |  deliveredAt: DateTime!
      |  readAt: DateTime!
      |}
      |
      |type Notification {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  type: NOTIFICATION_TYPE
      |  user: User!
      |  link: String!
      |  readDate: DateTime!
      |}
      |
      |type Restaurant {
      |  id: ID! @unique
      |  createdAt: DateTime!
      |  title: String!
      |  avgPricePerPerson: Int!
      |  pictures: [Picture]
      |  location: Location!
      |  isCurated: Boolean! @default(value: "true")
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

  val seedMutations = Vector(
    """mutation{createExperience(
    data: {
      popularity: 3
      pricePerPerson: 33
      title: "Raise a glass to Prohibition"
      host: {
        create: {
          email: "test2@test.com"
          firstName: "Kitty"
          lastName: "Miller"
          isSuperHost: true
          phone: "+1123455667"
          password: "secret"
          location: {
            create: {
              address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
              directions: "Follow the street to the end, then right"
              lat: 36.805235
              lng: 121.7892066
              neighbourHood: {
                create: {
                  name: "Monterey Countey"
                  slug: "monterey-countey"
                  featured: true
                  popularity: 1
                  city: { create: { name: "Moss Landing" } }
                }
              }
            }
          }
        }
      }
      preview: {
        create: {
          url: "https://a0.muscache.com/im/pictures/cb14d34d-65cf-401d-ba7f-585ec37b43ef.jpg"
        }
      }
      location: {
        create: {
          address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
          directions: "Follow the street to the end, then right"
          lat: 36.805235
          lng: 121.7892066
          neighbourHood: {
            create: {
              name: "Monterey Countey"
              slug: "monterey-countey"
              featured: true
              popularity: 1
              city: { create: { name: "Moss Landing" } }
            }
          }
        }
      }
    }
  ) {
    id
  }}""",
    """mutation{createRestaurant(
      |    data: {
      |      title: "Chumley's"
      |      pictures: {
      |        create: {
      |          url: "https://a0.muscache.com/pictures/a9a1d433-bcde-4601-88a0-5f16871b8548.jpg"
      |        }
      |      }
      |      slug: "chumleys"
      |      popularity: 1
      |      avgPricePerPerson: 30
      |      location: {
      |        create: {
      |          address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |          directions: "Follow the street to the end, then right"
      |          lat: 36.805235
      |          lng: 121.7892066
      |          neighbourHood: {
      |            create: {
      |              name: "Monterey Countey"
      |              slug: "monterey-countey"
      |              featured: true
      |              popularity: 1
      |              city: { create: { name: "Moss Landing" } }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  ) {
      |    id
      |  }}""",
    """mutation{createPlace(
      |    data: {
      |      name: "Mushroom Dome Cabin: #1 on airbnb in the world"
      |      shortDescription: "With a geodesic dome loft & a large deck in the trees, you'll feel like you're in a tree house in the woods. We are in a quiet yet convenient location. Shaded by Oak and Madrone trees and next to a Redwood grove, you can enjoy the outdoors from the deck. In the summer, it is cool and in the winter you might get to hear the creek running below."
      |      description: "The space\\n\\nWe have 10 acres next to land without fences so you will get to enjoy nature: just hang out on the deck, take a hike in the woods, watch the hummingbirds, pet the goats, go to the beach or gaze at the stars - as long as the moon isn't full. ; ) During the summer, if there isn't any nightly fog, we can see the Milky Way here.\\n\\nTo check our availability, click on the \"Request to Book\" link."
      |      maxGuests: 3
      |      pictures: {
      |        create: {
      |          url: "https://a0.muscache.com/im/pictures/140333/3ab8f121_original.jpg?aki_policy=xx_large"
      |        }
      |      }
      |      numBedrooms: 1
      |      numBeds: 3
      |      numBaths: 1
      |      size: ENTIRE_CABIN
      |      slug: "mushroom-dome"
      |      popularity: 1
      |      host: {
      |        create: {
      |          email: "test@test.com"
      |          firstName: "John"
      |          lastName: "Doe"
      |          isSuperHost: true
      |          phone: "+1123455667"
      |          password: "secret"
      |          location: {
      |            create: {
      |              address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |              directions: "Follow the street to the end, then right"
      |              lat: 36.805235
      |              lng: 121.7892066
      |              neighbourHood: {
      |                create: {
      |                  name: "Monterey Countey"
      |                  slug: "monterey-countey"
      |                  featured: true
      |                  popularity: 1
      |                  city: { create: { name: "Moss Landing" } }
      |                }
      |              }
      |            }
      |          }
      |        }
      |      }
      |      amenities: { create: { airConditioning: false, essentials: true } }
      |      views: { create: { lastWeek: 0 } }
      |      guestRequirements: {
      |        create: {
      |          recommendationsFromOtherHosts: false
      |          guestTripInformation: false
      |        }
      |      }
      |      policies: {
      |        create: {
      |          checkInStartTime: 11.00
      |          checkInEndTime: 20.00
      |          checkoutTime: 10.00
      |        }
      |      }
      |      pricing: {
      |        create: {
      |          averageMonthly: 1000
      |          averageWeekly: 300
      |          basePrice: 100
      |          cleaningFee: 30
      |          extraGuests: 80
      |          perNight: 100
      |          securityDeposit: 500
      |          weeklyDiscount: 50
      |          smartPricing: false
      |          currency: USD
      |        }
      |      }
      |      houseRules: {
      |        create: {
      |          smokingAllowed: false
      |          petsAllowed: true
      |          suitableForInfants: false
      |        }
      |      }
      |      location: {
      |        create: {
      |          address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |          directions: "Follow the street to the end, then right"
      |          lat: 36.805235
      |          lng: 121.7892066
      |          neighbourHood: {
      |            create: {
      |              name: "Monterey Countey"
      |              slug: "monterey-countey"
      |              featured: true
      |              popularity: 1
      |              city: { create: { name: "Moss Landing" } }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  ) {
      |    id
      |  }}""",
    """mutation{createPlace(
      |    data: {
      |      name: "Apartment 1 of 4 with green terrace in Roma Norte"
      |      shortDescription: "We offer other options with incredible terraces: Wonderful little loft with enjoyable terrace. Green and relaxing in the heart of the bustling city."
      |      description: "We offer other options with incredible terraces: Wonderful little loft with enjoyable terrace. Green and relaxing in the heart of the bustling city."
      |      maxGuests: 3
      |      pictures: {
      |        create: {
      |          url: "https://a0.muscache.com/im/pictures/45880516/93bb5931_original.jpg?aki_policy=xx_large"
      |        }
      |      }
      |      numBedrooms: 1
      |      numBeds: 3
      |      numBaths: 1
      |      size: ENTIRE_CABIN
      |      slug: "mushroom-dome"
      |      popularity: 1
      |      host: {
      |        create: {
      |          email: "test14@test.com"
      |          firstName: "Jason"
      |          lastName: "Padmakumara"
      |          isSuperHost: true
      |          phone: "+1123455667"
      |          password: "secret"
      |          location: {
      |            create: {
      |              address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |              directions: "Follow the street to the end, then right"
      |              lat: 36.805235
      |              lng: 121.7892066
      |              neighbourHood: {
      |                create: {
      |                  name: "Monterey Countey"
      |                  slug: "monterey-countey"
      |                  featured: true
      |                  popularity: 1
      |                  city: { create: { name: "Moss Landing" } }
      |                }
      |              }
      |            }
      |          }
      |        }
      |      }
      |      amenities: { create: { airConditioning: false, essentials: true } }
      |      views: { create: { lastWeek: 0 } }
      |      guestRequirements: {
      |        create: {
      |          recommendationsFromOtherHosts: false
      |          guestTripInformation: false
      |        }
      |      }
      |      policies: {
      |        create: {
      |          checkInStartTime: 11.00
      |          checkInEndTime: 20.00
      |          checkoutTime: 10.00
      |        }
      |      }
      |      pricing: {
      |        create: {
      |          averageMonthly: 1000
      |          averageWeekly: 300
      |          basePrice: 100
      |          cleaningFee: 30
      |          extraGuests: 80
      |          perNight: 110
      |          securityDeposit: 500
      |          weeklyDiscount: 50
      |          smartPricing: false
      |          currency: USD
      |        }
      |      }
      |      houseRules: {
      |        create: {
      |          smokingAllowed: false
      |          petsAllowed: true
      |          suitableForInfants: false
      |        }
      |      }
      |      location: {
      |        create: {
      |          address: "Narvarte Oriente, Mexico City, CDMX, Mexico"
      |          directions: "Follow the street to the end, then right"
      |          lat: 19.398095
      |          lng: -99.149452
      |          neighbourHood: {
      |            create: {
      |              name: "Mexico City"
      |              slug: "mexico-city"
      |              featured: true
      |              popularity: 1
      |              city: { create: { name: "Mexico City" } }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  ) {
      |    id
      |  }
      |}""",
    """mutation{createPlace(
      |    data: {
      |      name: "Urban Farmhouse at Curtis Park"
      |      shortDescription: "The Urban Farmhouse circa 1886 - meticulously converted in 2013. Situated adjacent to community garden. The updates afford you all the modern convenience you could ask for and charm you can only get from a building built in 1886. A true Charmer."
      |      description: "The Urban Farmhouse circa 1886 - meticulously converted in 2013. Situated adjacent to community garden. The updates afford you all the modern convenience you could ask for and charm you can only get from a building built in 1886. A true Charmer."
      |      maxGuests: 3
      |      pictures: {
      |        create: {
      |          url: "https://a0.muscache.com/im/pictures/ff6b760d-8782-4ccb-9e03-50aa720e3783.jpg?aki_policy=xx_large"
      |        }
      |      }
      |      numBedrooms: 1
      |      numBeds: 3
      |      numBaths: 1
      |      size: ENTIRE_CABIN
      |      slug: "mushroom-dome"
      |      popularity: 1
      |      host: {
      |        create: {
      |          email: "test12@test.com"
      |          firstName: "Hans"
      |          lastName: "Johanson"
      |          isSuperHost: true
      |          phone: "+1123455667"
      |          password: "secret"
      |          location: {
      |            create: {
      |              address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |              directions: "Follow the street to the end, then right"
      |              lat: 36.805235
      |              lng: 121.7892066
      |              neighbourHood: {
      |                create: {
      |                  name: "Monterey Countey"
      |                  slug: "monterey-countey"
      |                  featured: true
      |                  popularity: 1
      |                  city: { create: { name: "Moss Landing" } }
      |                }
      |              }
      |            }
      |          }
      |        }
      |      }
      |      amenities: { create: { airConditioning: false, essentials: true } }
      |      views: { create: { lastWeek: 0 } }
      |      guestRequirements: {
      |        create: {
      |          recommendationsFromOtherHosts: false
      |          guestTripInformation: false
      |        }
      |      }
      |      policies: {
      |        create: {
      |          checkInStartTime: 11.00
      |          checkInEndTime: 20.00
      |          checkoutTime: 10.00
      |        }
      |      }
      |      pricing: {
      |        create: {
      |          averageMonthly: 1000
      |          averageWeekly: 300
      |          basePrice: 100
      |          cleaningFee: 30
      |          extraGuests: 80
      |          perNight: 87
      |          securityDeposit: 500
      |          weeklyDiscount: 50
      |          smartPricing: false
      |          currency: USD
      |        }
      |      }
      |      houseRules: {
      |        create: {
      |          smokingAllowed: false
      |          petsAllowed: true
      |          suitableForInfants: false
      |        }
      |      }
      |      location: {
      |        create: {
      |          address: "W Crestline Ave, Littleton, CO 80120, USA"
      |          directions: "Follow the street to the end, then right"
      |          lat: 39.619115
      |          lng: -105.016560
      |          neighbourHood: {
      |            create: {
      |              name: "Denver"
      |              slug: "denver"
      |              featured: true
      |              popularity: 1
      |              city: { create: { name: "Denver" } }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  ) {
      |    id
      |  }}""",
    """mutation{createPlace(
      |    data: {
      |      name: "Underground Hygge"
      |      shortDescription: "This inspired dwelling nestled right into the breathtaking Columbia River Gorge mountainside. Reverently framed by the iconic round doorway, the wondrous views will entrance your imagination and inspire an unforgettable journey. Every nook of this little habitation will warm your sole, every cranny will charm your expedition of repose. Up the pathway, tucked into the earth, an unbelievable adventure awaits!"
      |      description: "This inspired dwelling nestled right into the breathtaking Columbia River Gorge mountainside. Reverently framed by the iconic round doorway, the wondrous views will entrance your imagination and inspire an unforgettable journey. Every nook of this little habitation will warm your sole, every cranny will charm your expedition of repose. Up the pathway, tucked into the earth, an unbelievable adventure awaits!"
      |      maxGuests: 3
      |      pictures: {
      |        create: {
      |          url: "https://a0.muscache.com/im/pictures/56bff280-aba3-42f3-af42-adc2814a72f4.jpg?aki_policy=xx_large"
      |        }
      |      }
      |      numBedrooms: 1
      |      numBeds: 3
      |      numBaths: 1
      |      size: ENTIRE_CABIN
      |      slug: "mushroom-dome"
      |      popularity: 1
      |      host: {
      |        create: {
      |          email: "test13@test.com"
      |          firstName: "Leah"
      |          lastName: "Dyer"
      |          isSuperHost: true
      |          phone: "+1123455667"
      |          password: "secret"
      |          location: {
      |            create: {
      |              address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |              directions: "Follow the street to the end, then right"
      |              lat: 36.805235
      |              lng: 121.7892066
      |              neighbourHood: {
      |                create: {
      |                  name: "Monterey Countey"
      |                  slug: "monterey-countey"
      |                  featured: true
      |                  popularity: 1
      |                  city: { create: { name: "Moss Landing" } }
      |                }
      |              }
      |            }
      |          }
      |        }
      |      }
      |      amenities: { create: { airConditioning: false, essentials: true } }
      |      views: { create: { lastWeek: 0 } }
      |      guestRequirements: {
      |        create: {
      |          recommendationsFromOtherHosts: false
      |          guestTripInformation: false
      |        }
      |      }
      |      policies: {
      |        create: {
      |          checkInStartTime: 11.00
      |          checkInEndTime: 20.00
      |          checkoutTime: 10.00
      |        }
      |      }
      |      pricing: {
      |        create: {
      |          averageMonthly: 1000
      |          averageWeekly: 300
      |          basePrice: 100
      |          cleaningFee: 30
      |          extraGuests: 80
      |          perNight: 69
      |          securityDeposit: 500
      |          weeklyDiscount: 50
      |          smartPricing: false
      |          currency: USD
      |        }
      |      }
      |      houseRules: {
      |        create: {
      |          smokingAllowed: false
      |          petsAllowed: true
      |          suitableForInfants: false
      |        }
      |      }
      |      location: {
      |        create: {
      |          address: "2600-2712 Entiat Way, Entiat, WA 98822, USA"
      |          directions: "Follow the street to the end, then right"
      |          lat: 47.631190
      |          lng: -120.220822
      |          neighbourHood: {
      |            create: {
      |              name: "Orondo"
      |              slug: "orondo"
      |              featured: true
      |              popularity: 1
      |              city: { create: { name: "Orondo" } }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  ) {
      |    id
      |  }
      |}""",
    """mutation{createPlace(
      |    data: {
      |      name: "Romantic, Cozy Cottage Next to Downtown"
      |      shortDescription: "Comfy, cozy and romantic cottage less than 3 miles from Downtown. The Cleveland Cottage provides a private oasis within city limits that includes full kitchen, Wifi, parking, electric fireplace & entrance through a private courtyard with fire pit."
      |      description: "Comfy, cozy and romantic cottage less than 3 miles from Downtown. The Cleveland Cottage provides a private oasis within city limits that includes full kitchen, Wifi, parking, electric fireplace & entrance through a private courtyard with fire pit."
      |      maxGuests: 3
      |      pictures: {
      |        create: {
      |          url: "https://a0.muscache.com/im/pictures/100298057/ccd8c843_original.jpg?aki_policy=xx_large"
      |        }
      |      }
      |      numBedrooms: 1
      |      numBeds: 3
      |      numBaths: 1
      |      size: ENTIRE_CABIN
      |      slug: "mushroom-dome"
      |      popularity: 1
      |      host: {
      |        create: {
      |          email: "test15@test.com"
      |          firstName: "Kris"
      |          lastName: "Mao"
      |          isSuperHost: true
      |          phone: "+1123455667"
      |          password: "secret"
      |          location: {
      |            create: {
      |              address: "2 Salmon Way, Moss Landing, Monterey County, CA, USA"
      |              directions: "Follow the street to the end, then right"
      |              lat: 36.805235
      |              lng: 121.7892066
      |              neighbourHood: {
      |                create: {
      |                  name: "Monterey Countey"
      |                  slug: "monterey-countey"
      |                  featured: true
      |                  popularity: 1
      |                  city: { create: { name: "Moss Landing" } }
      |                }
      |              }
      |            }
      |          }
      |        }
      |      }
      |      amenities: { create: { airConditioning: false, essentials: true } }
      |      views: { create: { lastWeek: 0 } }
      |      guestRequirements: {
      |        create: {
      |          recommendationsFromOtherHosts: false
      |          guestTripInformation: false
      |        }
      |      }
      |      policies: {
      |        create: {
      |          checkInStartTime: 11.00
      |          checkInEndTime: 20.00
      |          checkoutTime: 10.00
      |        }
      |      }
      |      pricing: {
      |        create: {
      |          averageMonthly: 1000
      |          averageWeekly: 300
      |          basePrice: 100
      |          cleaningFee: 30
      |          extraGuests: 80
      |          perNight: 110
      |          securityDeposit: 500
      |          weeklyDiscount: 50
      |          smartPricing: false
      |          currency: USD
      |        }
      |      }
      |      houseRules: {
      |        create: {
      |          smokingAllowed: false
      |          petsAllowed: true
      |          suitableForInfants: false
      |        }
      |      }
      |      location: {
      |        create: {
      |          address: "Greenwood, Nashville, TN 37206, USA"
      |          directions: "Follow the street to the end, then right"
      |          lat: 36.187977
      |          lng: -86.751635
      |          neighbourHood: {
      |            create: {
      |              name: "Nashville"
      |              slug: "nashville"
      |              featured: true
      |              popularity: 1
      |              city: { create: { name: "Nashville" } }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  ) {
      |    id
      |  }}"""
  )

}
