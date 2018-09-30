package com.prisma.api.mutations.nonEmbedded.nestedMutations

trait NestedMutationBase {

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
                            childrenOpt: [Child!]!
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentReq: Parent!
                        }"""

  val schemaPMToC1opt = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childrenOpt: [Child!]!
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentOpt: Parent
                        }"""

  val schemaP1reqToCM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childReq: Child!
                        }
                        
                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentsOpt: [Parent!]!
                        }"""

  val schemaP1optToCM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childOpt: Child
                        }

                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentsOpt: [Parent!]!
                        }"""

  val schemaPMToCM = """type Parent{
                            id: ID! @unique
                            p: String! @unique
                            childrenOpt: [Child!]!
                        }

                        type Child{
                            id: ID! @unique
                            c: String! @unique
                            parentsOpt: [Parent!]!
                        }"""
}
