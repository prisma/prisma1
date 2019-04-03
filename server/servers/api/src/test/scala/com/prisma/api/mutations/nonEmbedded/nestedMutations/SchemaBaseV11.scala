package com.prisma.api.mutations.nonEmbedded.nestedMutations
import com.prisma.api.TestDataModels

trait SchemaBaseV11 {

  //NON EMBEDDED A

  val schemaP1reqToC1req = {
    val s1 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childReq: Child! @relation(link: INLINE)
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentReq: Parent!
    }"""

    val s2 = """
    type Parent{
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

  val schemaP1optToC1req = {
    val s1 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childOpt: Child @relation(link: INLINE)
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentReq: Parent!
    }"""

    val s2 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childOpt: Child
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentReq: Parent! @relation(link: INLINE)
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaP1optToC1opt = {
    val s1 = """type Parent{
          id: ID! @id
          p: String! @unique
          childOpt: Child @relation(link: INLINE)
      }

      type Child{
          id: ID! @id
          c: String! @unique
          parentOpt: Parent
      }"""

    val s2 = """type Parent{
          id: ID! @id
          p: String! @unique
          childOpt: Child
      }

      type Child{
          id: ID! @id
          c: String! @unique
          parentOpt: Parent @relation(link: INLINE)
      }
          """

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaP1reqToC1opt = {
    val s1 = """type Parent{
          id: ID! @id
          p: String! @unique
          childReq: Child! @relation(link: INLINE)
      }
      
      type Child{
          id: ID! @id
          c: String! @unique
          parentOpt: Parent
      }"""

    val s2 = """type Parent{
          id: ID! @id
          p: String! @unique
          childReq: Child!
      }
      
      type Child{
          id: ID! @id
          c: String! @unique
          parentOpt: Parent @relation(link: INLINE)
      }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaPMToC1req = {
    val s1 = """
    type Parent{
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

    val s2 = """
    type Parent{
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

    val s3 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childrenOpt: [Child]
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentReq: Parent!
        test: String
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s2, s3))
  }

  val schemaPMToC1opt = {
    val s1 = """
    type Parent{
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

    val s2 = """
    type Parent{
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

    val s3 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childrenOpt: [Child]
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentOpt: Parent
        test: String
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s2, s3))
  }

  val schemaP1reqToCM = {
    val s1 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childReq: Child! @relation(link: INLINE) 
    }
    
    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent]
    }"""

    val s2 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childReq: Child! 
    }
    
    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent] @relation(link: INLINE)
    }"""

    val s3 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childReq: Child! 
    }
    
    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent]
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s3))
  }

  val schemaP1optToCM = {
    val s1 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childOpt: Child @relation(link: INLINE)
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent]
    }"""

    val s2 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childOpt: Child
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent] @relation(link: INLINE)
    }"""

    val s3 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childOpt: Child
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent]
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s3))
  }

  val schemaPMToCM = {
    val s1 = """
    type Parent{
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

    val s2 = """
    type Parent{
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

    val s3 = """
    type Parent{
        id: ID! @id
        p: String! @unique
        childrenOpt: [Child]
    }

    type Child{
        id: ID! @id
        c: String! @unique
        parentsOpt: [Parent]
        test: String
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s3))
  }

  //EMBEDDED

  val embeddedP1req = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child @embedded {
                            c: String!
                        }"""

  val embeddedP1opt = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childOpt: Child
                        }
                        
                        type Child @embedded {
                            c: String!
                        }"""

  val embeddedPM = """type Parent{
                            id: ID! @id
                            p: String! @unique
                            childrenOpt: [Child]
                        }
                        
                        type Child @embedded{
                            id: ID! @id
                            c: String!
                            test: String
                        }"""

  //EMBEDDED TO NON-EMBEDDED
  val embedddedToJoinFriendReq = """
                            |type Parent{
                            |    id: ID! @id
                            |    p: String @unique
                            |    children: [Child]
                            |}
                            |
                            |type Child @embedded {
                            |    id: ID! @id
                            |    c: String
                            |    friendReq: Friend!
                            |}
                            |
                            |type Friend{
                            |    id: ID! @id
                            |    f: String @unique
                            |}"""

  val embedddedToJoinFriendOpt = """
                               |type Parent{
                               |    id: ID! @id
                               |    p: String @unique
                               |    children: [Child]
                               |}
                               |
                               |type Child @embedded {
                               |    id: ID! @id
                               |    c: String
                               |    friendOpt: Friend
                               |}
                               |
                               |type Friend{
                               |    id: ID! @id
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
                        |    id: ID! @id
                        |    c: String
                        |    friendsOpt: [Friend]
                        |}
                        |
                        |type Friend{
                        |    id: ID! @id
                        |    f: String @unique
                        |    test: String
                        |}"""

}
