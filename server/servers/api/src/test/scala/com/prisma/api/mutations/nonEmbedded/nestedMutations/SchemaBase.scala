package com.prisma.api.mutations.nonEmbedded.nestedMutations

trait SchemaBase {

  //NON EMBEDDED

  val schemaP1reqToC1req = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentReq: Parent!
                        }"""

  val schemaP1optToC1req = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childOpt: Child
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentReq: Parent!
                        }"""

  val schemaP1optToC1opt = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childOpt: Child
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentOpt: Parent
                        }"""

  val schemaP1reqToC1opt = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentOpt: Parent
                        }"""

  val schemaPMToC1req = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childrenOpt: [Child]
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentReq: Parent!
                            test: String
                        }"""

  val schemaPMToC1opt = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childrenOpt: [Child]
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentOpt: Parent
                            test: String
                        }"""

  val schemaP1reqToCM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentsOpt: [Parent]
                        }"""

  val schemaP1optToCM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childOpt: Child
                        }

                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentsOpt: [Parent]
                        }"""

  val schemaPMToCM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childrenOpt: [Child]
                        }

                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentsOpt: [Parent]
                            test: String
                        }"""

  //EMBEDDED

  val embeddedP1req = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child @embedded {
                            c: String! @unique
                        }"""

  val embeddedP1opt = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childOpt: Child
                        }
                        
                        type Child @embedded {
                            c: String! @unique
                        }"""

  val embeddedPM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childrenOpt: [Child]
                        }
                        
                        type Child @embedded{
                            c: String! @unique
                            test: String
                        }"""

  //EMBEDDED TO NON-EMBEDDED
  val embedddedToJoinFriendReq = """type Parent{
                            |    p: String @unique
                            |    children: [Child]
                            |}
                            |
                            |type Child @embedded {
                            |    c: String @unique
                            |    friendReq: Friend! @mongoRelation(field: "friends")
                            |}
                            |
                            |type Friend{
                            |    f: String @unique
                            |}"""

  val embedddedToJoinFriendOpt = """type Parent{
                               |    p: String @unique
                               |    children: [Child]
                               |}
                               |
                               |type Child @embedded {
                               |    c: String @unique
                               |    friendOpt: Friend @mongoRelation(field: "friends")
                               |}
                               |
                               |type Friend{
                               |    f: String @unique
                               |}"""

  val embedddedToJoinFriendsOpt = """
                        |type Parent{
                        |    id: ID! @unique
                        |    p: String @unique
                        |    children: [Child]
                        |}
                        |
                        |type Child @embedded {
                        |    c: String @unique
                        |    friendsOpt: [Friend] @mongoRelation(field: "friends")
                        |}
                        |
                        |type Friend{
                        |    f: String @unique
                        |    test: String
                        |}"""

}
