package com.prisma.gc_values

import org.scalactic.{Bad, Good, Or}

trait GCConverter[T] extends ToGcValue[T]

trait ToGcValue[T] {
  def toGCValue(t: T): Or[GCValue, InvalidValueForScalarType]
}

case class InvalidValueForScalarType(value: String, typeIdentifier: String)

object OtherGCStuff {

  /**
    * This helps convert Or listvalues.
    */
  def sequence[A, B](seq: Vector[Or[A, B]]): Or[Vector[A], B] = {
    def recurse(seq: Vector[Or[A, B]])(acc: Vector[A]): Or[Vector[A], B] = {
      if (seq.isEmpty) {
        Good(acc)
      } else {
        seq.head match {
          case Good(x)    => recurse(seq.tail)(acc :+ x)
          case Bad(error) => Bad(error)
        }
      }
    }
    recurse(seq)(Vector.empty)
  }
}
