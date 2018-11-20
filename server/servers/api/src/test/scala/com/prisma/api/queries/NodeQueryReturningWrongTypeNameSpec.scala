package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, NodeQueryCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NodeQueryReturningWrongTypeNameSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(NodeQueryCapability, JoinRelationLinksCapability)

  "the node query" should "return the correct typename" in {
    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  person: Person!
        |}
        |
        |type Person {
        |  id: ID! @unique
        |  email: String
        |  firstName: String
        |  lastName: String
        |  phoneNumbers: [PhoneNumber]
        |  scheduledTransactions: [FinancialScheduledTransaction]
        |  transactions: [FinancialTransaction]
        |  user: User
        |}
        |
        |type PhoneNumber {
        |  id: ID! @unique
        |  number: String!
        |}
        |
        |type FinancialAccount {
        |  id: ID! @unique
        |  key: String @unique
        |  campuses: [Campus]
        |  description: String!
        |  isActive: Boolean!
        |  name: String
        |}
        |
        |enum CREDIT_CARD {
        |  VISA
        |  MASTERCARD
        |  AMEX
        |  DISCOVER
        |}
        |
        |enum CURRENCY_TYPE {
        |  ACH
        |  CC
        |}
        |
        |enum ACH_TYPE {
        |  SAVINGS
        |  CHECKING
        |}
        |
        |type FinancialPaymentDetail {
        |  id: ID! @unique
        |  accountNumberMasked: String!
        |  billingLocation: Location
        |  creditCardType: CREDIT_CARD
        |  achType: ACH_TYPE
        |  currencyType: CURRENCY_TYPE
        |  expirationDate: DateTime!
        |  nameOnCard: String
        |}
        |
        |enum TRANSACTION_FREQUENCY {
        |  DAILY
        |  MONTHLY
        |  BIWEEKLY
        |  CUSTOM
        |}
        |
        |type FinancialScheduledTransaction {
        |  id: ID! @unique
        |  person: Person
        |  endDate: DateTime
        |  payment: FinancialPaymentDetail
        |  isActive: Boolean!
        |  startDate: DateTime
        |  frequency: TRANSACTION_FREQUENCY
        |  transactions: [FinancialTransaction]
        |  account: FinancialAccount
        |  amount: Float!
        |}
        |
        |enum TRANSACTION_STATUS {
        |  PENDING
        |  SUCCESS
        |  FAILED
        |}
        |
        |type FinancialTransaction {
        |  id: ID! @unique
        |  payment: FinancialPaymentDetail
        |  person: Person
        |  processedDate: DateTime
        |  scheduledTransaction: FinancialScheduledTransaction
        |  status: TRANSACTION_STATUS!
        |  transactionDate: DateTime
        |  amount: Float!
        |  account: FinancialAccount
        |  organization: Group!
        |}
        |
        |enum GROUP_INVITE_STATUS {
        |  PENDING
        |  JOINED
        |}
        |
        |enum GROUP_ROLE_TYPE {
        |  ADMIN
        |  OWNER
        |  MEMBER
        |}
        |
        |type GroupInvite {
        |  id: ID! @unique
        |  email: String!
        |  group: Group!
        |  groupRole: GroupRole
        |  status: GROUP_INVITE_STATUS!
        |}
        |
        |type GroupMember {
        |  id: ID! @unique
        |  group: Group
        |  role: GroupRole
        |  person: Person
        |}
        |
        |type GroupRole {
        |  id: ID! @unique
        |  canEdit: Boolean!
        |  canView: Boolean!
        |  description: String!
        |  groupType: GroupType
        |  isLeader: Boolean
        |  name: String! @unique
        |  type: GROUP_ROLE_TYPE
        |}
        |
        |type GroupType {
        |  id: ID! @unique
        |  description: String
        |  groups: [Group]
        |  name: String! @unique
        |  roles: [GroupRole]
        |}
        |
        |type Group {
        |  id: ID! @unique
        |  key: String @unique
        |  description: String
        |  type: GroupType!
        |  invites: [GroupInvite]
        |  isActive: Boolean!
        |  members: [GroupMember]
        |  name: String!
        |  organization: Group
        |}
        |
        |type Campus {
        |  id: ID! @unique
        |  accounts: [FinancialAccount]
        |  description: String
        |  isActive: Boolean
        |  organization: Group
        |  location: Location
        |  name: String!
        |  phoneNumbers: [PhoneNumber]
        |}
        |
        |enum LOCATION_TYPE {
        |  HOME
        |  WORK
        |}
        |
        |type Location {
        |  id: ID! @unique
        |  city: String
        |  locationType: LOCATION_TYPE
        |  postalCode: String
        |  state: String
        |  street1: String
        |  street2: String
        |}
      """.stripMargin
    }

    database.setup(project)

    server.query(
      """mutation {
                   |  createGroupType(
                   |    data: {
                   |      name: "Organization"
                   |      description: "An Organization"
                   |      roles: {
                   |        create: [
                   |          {
                   |            canEdit: false
                   |            canView: true
                   |            description: "Org Member"
                   |            isLeader: false
                   |            name: "Member"
                   |            type: MEMBER
                   |          }
                   |          {
                   |            canEdit: true
                   |            canView: true
                   |            description: "Org Admin"
                   |            isLeader: false
                   |            name: "Admin"
                   |            type: ADMIN
                   |          }
                   |          {
                   |            canEdit: true
                   |            canView: true
                   |            description: "Org Owner"
                   |            isLeader: true
                   |            name: "Owner"
                   |            type: OWNER
                   |          }
                   |        ]
                   |      }
                   |    }
                   |  ) {
                   |    id
                   |  }
                   |
                   |  createGroup(
                   |    data: {
                   |      name: "First Presbyterian Church"
                   |      key: "First Presbyterian Church"
                   |      description: "First Presbyterian Church"
                   |      isActive: true
                   |      type: {connect: {name: "Organization"}}
                   |      members: {
                   |        create: [
                   |          {
                   |            role: {connect: {name: "Owner"}}
                   |            person: {create: {firstName: "Fred", lastName: "Fredrickson"}}
                   |          }
                   |          {
                   |            role: {connect: {name: "Admin"}}
                   |            person: {create: {firstName: "Sally", lastName: "Sanderson"}}
                   |          }
                   |        ]
                   |      }
                   |    }
                   |  ) {
                   |    id
                   |  }
                   |
                   |  createCampus(
                   |    data: {
                   |      name: "First Presbyterian Church - Greenville"
                   |      isActive: true
                   |      location: {
                   |        create: {
                   |          street1: "123 Ministry Street"
                   |          city: "Greenville"
                   |          state: "SC"
                   |          postalCode: "12345"
                   |        }
                   |      }
                   |      phoneNumbers: {create: [{number: "1234567890"}]}
                   |      organization: {connect: {key: "First Presbyterian Church"}}
                   |      accounts: {
                   |        create: [
                   |          {
                   |            name: "Greenville General Fund"
                   |            description: "GVL General Fund"
                   |            isActive: true
                   |            key: "GVL General Account"
                   |          }
                   |        ]
                   |      }
                   |    }
                   |  ) {
                   |    id
                   |  }
                   |
                   |  createPerson(
                   |    data: {
                   |      firstName: "Joe"
                   |      lastName: "Josephson"
                   |      transactions: {
                   |        create: [
                   |          {
                   |            organization: {connect: {key: "First Presbyterian Church"}}
                   |            account: {connect: {key: "GVL General Account"}}
                   |            status: SUCCESS
                   |            amount: 200.00
                   |            transactionDate: "2018-04-01T00:00:00.000Z"
                   |            processedDate: "2018-04-01T00:01:00.000Z"
                   |            payment: {
                   |              create: {
                   |                accountNumberMasked: "1234"
                   |                expirationDate: "2020-12-01"
                   |                creditCardType: MASTERCARD
                   |                nameOnCard: "Joe Josephson"
                   |              }
                   |            }
                   |          }
                   |        ]
                   |      }
                   |    }
                   |  ) {
                   |    id
                   |  }
                   |}""",
      project
    )

    val campusId = server
      .query(
        s"""query {
      campuses {
        id
      }
    }""",
        project
      )
      .pathAsString("data.campuses.[0].id")

    val result = server.query(
      s"""{
         |  node(id: "$campusId"){
         |    __typename
         |  }
         |}""",
      project
    )

    result.toString should equal("""{"data":{"node":{"__typename":"Campus"}}}""")
  }

}
