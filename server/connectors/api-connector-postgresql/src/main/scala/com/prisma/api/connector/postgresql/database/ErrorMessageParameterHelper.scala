package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.NodeSelector
import com.prisma.gc_values._

object ErrorMessageParameterHelper {

  /**
    * This is used to parse SQL exceptions for references of specific GCValues
    */
  def parameterString(where: NodeSelector) = where.fieldValue match {
    case StringGCValue(x)      => s"parameters ['$x',"
    case IntGCValue(x)         => s"parameters [$x,"
    case FloatGCValue(x)       => s"parameters [$x,"
    case BooleanGCValue(false) => s"parameters [0,"
    case BooleanGCValue(true)  => s"parameters [1,"
    case IdGCValue(x)          => s"parameters ['$x',"
    case EnumGCValue(x)        => s"parameters ['$x',"
    case DateTimeGCValue(x)    => s"parameters ['${dateTimeFromISO8601(x)}"
    case JsonGCValue(x)        => s"parameters ['$x'," // Todo
    case ListGCValue(_)        => sys.error("Not an acceptable Where")
    case RootGCValue(_)        => sys.error("Not an acceptable Where")
    case NullGCValue           => sys.error("Not an acceptable Where")
  }

  private def dateTimeFromISO8601(v: Any) = {
    val string = v.toString
    //"2017-12-05T12:34:23.000Z" to "2017-12-05T12:34:23.000" which MySQL will accept
    string.replace("T", " ").substring(0, string.length - 3) //todo pretty hacky
  }

}
