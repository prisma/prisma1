package cool.graph.system.database.tables
import cool.graph.shared.models.FieldConstraintType
import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import slick.jdbc.MySQLProfile.api._

case class FieldConstraint(
    id: String,
    constraintType: FieldConstraintType,
    equalsNumber: Option[Double] = None,
    oneOfNumber: String = "[]",
    min: Option[Double] = None,
    max: Option[Double] = None,
    exclusiveMin: Option[Double] = None,
    exclusiveMax: Option[Double] = None,
    multipleOf: Option[Double] = None,
    equalsString: Option[String] = None,
    oneOfString: String = "[]",
    minLength: Option[Int] = None,
    maxLength: Option[Int] = None,
    startsWith: Option[String] = None,
    endsWith: Option[String] = None,
    includes: Option[String] = None,
    regex: Option[String] = None,
    equalsBoolean: Option[Boolean] = None,
    uniqueItems: Option[Boolean] = None,
    minItems: Option[Int] = None,
    maxItems: Option[Int] = None,
    fieldId: String
)

class FieldConstraintTable(tag: Tag) extends Table[FieldConstraint](tag, "FieldConstraint") {

  implicit val FieldConstraintTypeMapper = FieldConstraintTable.FieldConstraintTypeMapper

  def id             = column[String]("id", O.PrimaryKey)
  def constraintType = column[FieldConstraintType]("constraintType")
  def equalsNumber   = column[Option[Double]]("equalsNumber")
  def oneOfNumber    = column[String]("oneOfNumber")
  def min            = column[Option[Double]]("min")
  def max            = column[Option[Double]]("max")
  def exclusiveMin   = column[Option[Double]]("exclusiveMin")
  def exclusiveMax   = column[Option[Double]]("exclusiveMax")
  def multipleOf     = column[Option[Double]]("multipleOf")
  def equalsString   = column[Option[String]]("equalsString")
  def oneOfString    = column[String]("oneOfString")
  def minLength      = column[Option[Int]]("minLength")
  def maxLength      = column[Option[Int]]("maxLength")
  def startsWith     = column[Option[String]]("startsWith")
  def endsWith       = column[Option[String]]("endsWith")
  def includes       = column[Option[String]]("includes")
  def regex          = column[Option[String]]("regex")
  def equalsBoolean  = column[Option[Boolean]]("equalsBoolean")
  def uniqueItems    = column[Option[Boolean]]("uniqueItems")
  def minItems       = column[Option[Int]]("minItems")
  def maxItems       = column[Option[Int]]("maxItems")

  def fieldId = column[String]("fieldId")
  def field   = foreignKey("fieldConstraint_fieldid_foreign", fieldId, Tables.Fields)(_.id)

  def * =
    (id,
     constraintType,
     equalsNumber,
     oneOfNumber,
     min,
     max,
     exclusiveMin,
     exclusiveMax,
     multipleOf,
     equalsString,
     oneOfString,
     minLength,
     maxLength,
     startsWith,
     endsWith,
     includes,
     regex,
     equalsBoolean,
     uniqueItems,
     minItems,
     maxItems,
     fieldId) <> ((FieldConstraint.apply _).tupled, FieldConstraint.unapply)
}

object FieldConstraintTable {
  implicit val FieldConstraintTypeMapper =
    MappedColumnType.base[FieldConstraintType, String](
      e => e.toString,
      s => FieldConstraintType.withName(s)
    )
}
