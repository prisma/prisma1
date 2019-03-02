package com.prisma.deploy.connector

import com.prisma.gc_values._
import com.prisma.shared.models.{ScalarField, TypeIdentifier}
import org.joda.time.DateTime
import play.api.libs.json._

trait MigrationValueGenerator {
  def migrationValueForField(field: ScalarField): GCValue = field.typeIdentifier match {
    case _ if field.defaultValue.isDefined => field.defaultValue.get
    case TypeIdentifier.String             => StringGCValue("")
    case TypeIdentifier.Boolean            => BooleanGCValue(false)
    case TypeIdentifier.Int                => IntGCValue(0)
    case TypeIdentifier.Float              => FloatGCValue(0.0)
    case TypeIdentifier.DateTime           => DateTimeGCValue(new DateTime("1970-01-01T00:00:00Z"))
    case TypeIdentifier.Json               => JsonGCValue(Json.parse("{}"))
    case TypeIdentifier.Enum               => EnumGCValue(field.enum.get.values.head)
    case _                                 => sys.error("MigrationValue method should only be called on scalar fields.")
  }
}
