package util

trait SchemaBaseV11 {

  //NON EMBEDDED A

  val schemaP1reqToC1req = {
    val s1 = """
    model Parent {
        id       String @id @default(cuid())
        p        String @unique
        childReq Child  @relation(link: INLINE)
    }

    model Child {
        id        ID     @id
        c         String @unique
        parentReq Parent
    }"""

    val s2 = """
    model Parent {
        id       String @id @default(cuid())
        p        String @unique
        childReq Child
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentReq Parent @relation(link: INLINE)
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaP1optToC1req = {
    val s1 = """
    model Parent {
        id       String @id @default(cuid())
        p        String @unique
        childOpt Child? @relation(link: INLINE)
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentReq Parent
    }"""

    val s2 = """
    model Parent {
        id       String @id @default(cuid())
        p        String @unique
        childOpt Child?
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentReq Parent @relation(link: INLINE)
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaP1optToC1opt = {
    val s1 = """
      model Parent {
          id       String @id @default(cuid())
          p        String @unique
          childOpt Child? @relation(references: [id])
      }

      model Child {
          id        String @id @default(cuid())
          c         String @unique
          parentOpt Parent?
      }"""

    val s2 = """
      model Parent {
          id       String @id @default(cuid())
          p        String @unique
          childOpt Child?
      }

      model Child {
          id        String  @id @default(cuid())
          c         String  @unique
          parentOpt Parent? @relation(references: [id])
      }
          """

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaP1reqToC1opt = {
    val s1 = """
      model Parent {
          id       String @id @default(cuid())
          p        String @unique
          childReq Child  @relation(link: INLINE)
      }
      
      model Child {
          id        String  @id @default(cuid())
          c         String  @unique
          parentOpt Parent?
      }"""

    val s2 = """
      model Parent {
          id       String @id @default(cuid())
          p        String @unique
          childReq Child
      }
      
      model Child {
          id        String  @id @default(cuid())
          c         String  @unique
          parentOpt Parent? @relation(link: INLINE)
      }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s2))
  }

  val schemaPMToC1req = {
    val s1 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child] @relation(link: INLINE)
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentReq Parent
        test      String
    }"""

    val s2 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child]
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentReq Parent @relation(link: INLINE)
        test      String
    }"""

    val s3 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child]
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentReq Parent
        test      String
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s2, s3))
  }

  val schemaPMToC1opt = {
    val s1 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child] @relation(link: INLINE)
    }

    model Child {
        id        String @id @default(cuid())
        c         String @unique
        parentOpt Parent?
        test      String
    }"""

    val s2 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child]
    }

    model Child {
        id        String  @id @default(cuid())
        c         String  @unique
        parentOpt Parent? @relation(link: INLINE)
        test      String
    }"""

    val s3 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child]
    }

    model Child {
        id        String  @id @default(cuid())
        c         String  @unique
        parentOpt Parent?
        test      String
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s2, s3))
  }

  val schemaP1reqToCM = {
    val s1 = """
    model Parent {
        id       String  @id @default(cuid())
        p        String? @unique
        childReq Child   @relation(link: INLINE)
    }
    
    model Child {
        id         String  @id @default(cuid())
        c          String  @unique
        parentsOpt [Parent]
    }"""

    val s2 = """
    model Parent {
        id       String  @id @default(cuid())
        p        String? @unique
        childReq Child 
    }
    
    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent] @relation(link: INLINE)
    }"""

    val s3 = """
    model Parent {
        id       String  @id @default(cuid())
        p        String? @unique
        childReq Child 
    }
    
    model Child {
        id         String  @id @default(cuid())
        c          String  @unique
        parentsOpt [Parent]
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s3))
  }

  val schemaP1optToCM = {
    val s1 = """
    model Parent {
        id       String  @id @default(cuid())
        p        String? @unique
        childOpt Child?  @relation(link: INLINE)
    }

    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent]
    }"""

    val s2 = """
    model Parent {
        id       String  @id @default(cuid())
        p        String? @unique
        childOpt Child?
    }

    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent] @relation(link: INLINE)
    }"""

    val s3 = """
    model Parent {
        id       String  @id @default(cuid())
        p        String? @unique
        childOpt Child?
    }

    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent]
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s1, s3))
  }

  val schemaPMToCM = {
    val s1 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child] @relation(link: INLINE)
    }

    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent]
        test       String
    }"""

    val s2 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child]
    }

    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent] @relation(link: INLINE)
        test       String
    }"""

    val s3 = """
    model Parent {
        id          String  @id @default(cuid())
        p           String? @unique
        childrenOpt [Child]
    }

    model Child {
        id         String   @id @default(cuid())
        c          String   @unique
        parentsOpt [Parent]
        test       String
    }"""

    TestDataModels(mongo = Vector(s1, s2), sql = Vector(s3))
  }

  //EMBEDDED

  val embeddedP1req = """model Parent {
                            id       String  @id @default(cuid())
                            p        String? @unique
                            childReq Child
                        }
                        
                        model Child @embedded {
                            c String
                        }"""

  val embeddedP1opt = """model Parent {
                            id       String  @id @default(cuid())
                            p        String? @unique
                            childOpt Child?
                        }
                        
                        model Child @embedded {
                            c String
                        }"""

  val embeddedPM = """model Parent {
                            id          String  @id @default(cuid())
                            p           String? @unique
                            childrenOpt [Child]
                        }
                        
                        model Child @embedded{
                            id   String @id @default(cuid())
                            c    String
                            test String
                        }"""

  //EMBEDDED TO NON-EMBEDDED
  val embedddedToJoinFriendReq = """
                            |model Parent {
                            |    id       String  @id @default(cuid())
                            |    p        String? @unique
                            |    children [Child]
                            |}
                            |
                            |model Child @embedded {
                            |    id        String @id @default(cuid())
                            |    c         String
                            |    friendReq Friend
                            |}
                            |
                            |model Friend{
                            |    id String @id @default(cuid())
                            |    f  String @unique
                            |}"""

  val embedddedToJoinFriendOpt = """
                               |model Parent {
                               |    id       String  @id @default(cuid())
                               |    p        String? @unique
                               |    children [Child]
                               |}
                               |
                               |model Child @embedded {
                               |    id        String  @id @default(cuid())
                               |    c         String
                               |    friendOpt Friend?
                               |}
                               |
                               |model Friend{
                               |    id String @id @default(cuid())
                               |    f  String @unique
                               |}"""

  val embedddedToJoinFriendsOpt = """
                        |model Parent {
                        |    id       String  @id @default(cuid())
                        |    p        String? @unique
                        |    children [Child]
                        |}
                        |
                        |model Child @embedded {
                        |    id         String @id @default(cuid())
                        |    c          String
                        |    friendsOpt [Friend]
                        |}
                        |
                        |model Friend{
                        |    id   String @id @default(cuid())
                        |    f    String @unique
                        |    test String
                        |}"""

}
