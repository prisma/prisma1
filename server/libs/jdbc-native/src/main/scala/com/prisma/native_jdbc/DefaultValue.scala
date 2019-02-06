package com.prisma.native_jdbc

sealed trait DefaultValue[T] {
  def default: T
}
object DefaultValues {
  object NullDefaultValue             extends DefaultValue[Null]    { def default = null  }
  implicit object IntDefaultValue     extends DefaultValue[Int]     { def default = 0     }
  implicit object LongDefaultValue    extends DefaultValue[Long]    { def default = 0l    }
  implicit object BooleanDefaultValue extends DefaultValue[Boolean] { def default = false }
  implicit object DoubleDefaultValue  extends DefaultValue[Double]  { def default = 0f    }
  implicit object FloatDefaultValue   extends DefaultValue[Float]   { def default = 0f    }

  implicit def nullDefault[T >: Null]: DefaultValue[T] = NullDefaultValue.asInstanceOf[DefaultValue[T]]
}
