package com.prisma.stub

import java.util.function.BinaryOperator
import javax.servlet.http.HttpServletRequest

object JavaServletRequest {
  def body(request: HttpServletRequest): String = {
    val reduceFn = new BinaryOperator[String] {
      override def apply(acc: String, actual: String): String = acc + actual
    }
    request.getReader().lines().reduce("", reduceFn)
  }

  def headers(request: HttpServletRequest): Map[String, String] = {
    import scala.collection.mutable
    val map: mutable.Map[String, String] = mutable.Map.empty
    val headerNames                      = request.getHeaderNames

    while (headerNames.hasMoreElements) {
      val key = Option(headerNames.nextElement())
      val value = for {
        k <- key
        v <- Option(request.getHeader(k))
      } yield v

      (key, value) match {
        case (Some(k), Some(v)) => map.put(k, v)
        case _                  => ()
      }
    }
    map.toMap
  }
}
