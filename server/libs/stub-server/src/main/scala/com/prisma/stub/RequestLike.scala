package com.prisma.stub

import scala.collection.SortedMap

trait RequestLike {
  def httpMethod: String
  def path: String
  def queryMap: Map[String, Any]
  def body: String

  val querySortedMap: SortedMap[String, Any] = QueryString.mapToSortedMap(queryMap)
  val queryString: String                    = QueryString.queryMapToString(queryMap)

  def isPostOrPatch: Boolean = httpMethod.equalsIgnoreCase("POST") || httpMethod.equalsIgnoreCase("PATCH")
}
