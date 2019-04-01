package com.prisma.api.mutations.nonEmbedded.nestedMutations
import com.prisma.TestDataModels

trait SchemaBasev11 {

  //NON EMBEDDED A

  val schemaP1reqToC1reqA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childReq: Child! @relation(link: INLINE)
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentReq: Parent!
                        }"""

  val schemaP1reqToC1reqA_foo = {
    val s1 = """type Parent{
          id: ID! @id
          p: String! @unique
          childReq: Child! @relation(link: INLINE)
      }

      type Child{
          id: ID! @id
          c: String! @unique
          parentReq: Parent!
      }"""

    val s2 = """type Parent{
          id: ID! @id
          p: String! @unique
          childReq: Child!
      }

      type Child{
          id: ID! @id
          c: String! @unique
          parentReq: Parent! @relation(link: INLINE)
      }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaP1optToC1reqA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child @relation(link: INLINE)
                        }
                        
                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentReq: Parent!
                        }"""

  val schemaP1optToC1optA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child @relation(link: INLINE)
                        }
                        
                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentOpt: Parent
                        }"""

  val schemaP1reqToC1optA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childReq: Child! @relation(link: INLINE)
                        }
                        
                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentOpt: Parent
                        }"""

  val schemaPMToC1reqA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child] @relation(link: INLINE)
                        }
                        
                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentReq: Parent!
                            test: String
                        }"""

  val schemaPMToC1optA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child] @relation(link: INLINE)
                        }
                        
                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentOpt: Parent
                            test: String
                        }"""

  val schemaP1reqToCMA = {
    val dm1 = """type Parent{
                    id: ID! @id
                    p: String! @unique
                    childReq: Child! 
                }
                
                type Child{
                    id: ID! @id
                    c: String! @unique
                    parentsOpt: [Parent]
                }"""

    val dm2 = """type Parent{
                    id: ID! @id
                    p: String! @unique
                    childReq: Child! @relation(link: INLINE)
                }
                
                type Child{
                    id: ID! @id
                    c: String! @unique
                    parentsOpt: [Parent]
                }"""

    val dm3 = """type Parent{
                    id: ID! @id
                    p: String! @unique
                    childReq: Child! 
                }
                
                type Child{
                    id: ID! @id
                    c: String! @unique
                    parentsOpt: [Parent] @relation(link: INLINE)
                }"""

    TestDataModels(mongo = Vector(dm2, dm3), sql = Vector(dm1))
  }

  val schemaP1optToCMA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child @relation(link: INLINE)
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentsOpt: [Parent]
                        }"""

  val schemaPMToCMA = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child] @relation(link: INLINE)
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentsOpt: [Parent]
                            test: String
                        }"""

  //NON-EMBEDDED B

  val schemaP1reqToC1reqB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childReq: Child!
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentReq: Parent! @relation(link: INLINE)
                        }"""

  val schemaP1optToC1reqB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentReq: Parent! @relation(link: INLINE)
                        }"""

  val schemaP1optToC1optB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentOpt: Parent @relation(link: INLINE)
                        }"""

  val schemaP1reqToC1optB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childReq: Child!
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentOpt: Parent @relation(link: INLINE)
                        }"""

  val schemaPMToC1reqB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child]
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentReq: Parent! @relation(link: INLINE)
                            test: String
                        }"""

  val schemaPMToC1optB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child]
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentOpt: Parent @relation(link: INLINE)
                            test: String
                        }"""

  val schemaP1optToCMB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentsOpt: [Parent] @relation(link: INLINE)
                        }"""

  val schemaPMToCMB = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child]
                        }

                        type Child{
                            id: ID! @id
                            c: String! @unique
                            parentsOpt: [Parent] @relation(link: INLINE)
                            test: String
                        }"""

  //EMBEDDED

  val embeddedP1req = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child @embedded {
                            c: String! @unique
                        }"""

  val embeddedP1opt = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child
                        }
                        
                        type Child @embedded {
                            c: String! @unique
                        }"""

  val embeddedPM = """type Parent{
                            id: ID! @id
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
                        |    id: ID! @id
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
