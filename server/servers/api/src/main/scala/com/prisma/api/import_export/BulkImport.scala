package com.prisma.api.import_export

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.CoolArgs
import com.prisma.api.connector.mysql.database.{DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport._
import com.prisma.shared.models._
import cool.graph.cuid.Cuid
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import spray.json._

import scala.concurrent.Future
import scala.util.Try

class BulkImport(project: Project)(implicit apiDependencies: ApiDependencies) {

  val db = apiDependencies.databases

  def executeImport(json: JsValue): Future[JsValue] = {
    import apiDependencies.system.dispatcher

    val bundle = json.convertTo[ImportBundle]
    val count  = bundle.values.elements.length

    val actions =
      bundle.valueType match {
        case "nodes"     => generateImportNodesDBActions(bundle.values.elements.map(convertToImportNode))
        case "relations" => generateImportRelationsDBActions(bundle.values.elements.map(convertToImportRelation))
        case "lists"     => generateImportListsDBActions(bundle.values.elements.map(convertToImportList))

      }

    val res: Future[Vector[Try[Int]]] = runDBActions(actions)

    def messageWithOutConnection(tryelem: Try[Any]): String = tryelem.failed.get.getMessage.substring(tryelem.failed.get.getMessage.indexOf(")") + 1)

    res
      .map(vector =>
        vector.zipWithIndex.collect {
          case (elem, idx) if elem.isFailure && idx < count  => Map("index" -> idx, "message"           -> messageWithOutConnection(elem)).toJson
          case (elem, idx) if elem.isFailure && idx >= count => Map("index" -> (idx - count), "message" -> messageWithOutConnection(elem)).toJson
      })
      .map(x => JsArray(x))
  }

  private def getImportIdentifier(map: Map[String, Any]): ImportIdentifier =
    ImportIdentifier(map("_typeName").asInstanceOf[String], map("id").asInstanceOf[String])

  private def convertToImportNode(json: JsValue): ImportNode = {
    val map      = json.convertTo[Map[String, Any]]
    val valueMap = map.collect { case (k, v) if k != "_typeName" && k != "id" => (k, v) }

    ImportNode(getImportIdentifier(map), valueMap)
  }

  private def convertToImportList(json: JsValue): ImportList = {
    val map      = json.convertTo[Map[String, Any]]
    val valueMap = map.collect { case (k, v) if k != "_typeName" && k != "id" => (k, v.asInstanceOf[List[Any]].toVector) }

    ImportList(getImportIdentifier(map), valueMap)
  }

  private def convertToImportRelation(json: JsValue): ImportRelation = {
    val array    = json.convertTo[JsArray]
    val leftMap  = array.elements.head.convertTo[Map[String, Option[String]]]
    val rightMap = array.elements.last.convertTo[Map[String, Option[String]]]
    val left     = ImportRelationSide(ImportIdentifier(leftMap("_typeName").get, leftMap("id").get), leftMap.get("fieldName").flatten)
    val right    = ImportRelationSide(ImportIdentifier(rightMap("_typeName").get, rightMap("id").get), rightMap.get("fieldName").flatten)

    ImportRelation(left, right)
  }

  private def dateTimeFromISO8601(v: Any) = {
    val string = v.asInstanceOf[String]
    //"2017-12-05T12:34:23.000Z" to "2017-12-05 12:34:23.000 " which MySQL will accept
    string.replace("T", " ").replace("Z", " ")
  }

  private def generateImportNodesDBActions(nodes: Vector[ImportNode]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
    val items = nodes.map { element =>
      val id    = element.identifier.id
      val model = project.schema.getModelByName_!(element.identifier.typeName)

      val formatedValues = element.values.map {
        case (k, v) if k == "createdAt" || k == "updatedAt"                                => (k, dateTimeFromISO8601(v))
        case (k, v) if !model.fields.map(_.name).contains(k)                               => (k, v) // let it fail at db level
        case (k, v) if model.getFieldByName_!(k).typeIdentifier == TypeIdentifier.DateTime => (k, dateTimeFromISO8601(v))
        case (k, v) if model.getFieldByName_!(k).typeIdentifier == TypeIdentifier.Json     => (k, v.toJson)
        case (k, v)                                                                        => (k, v)
      }

      val values: CoolArgs = CoolArgs(formatedValues + ("id" -> id))

      DatabaseMutationBuilder.createDataItem(project.id, model.name, values).asTry
    }

    val relayIds: TableQuery[ProjectRelayIdTable] = TableQuery(new ProjectRelayIdTable(_, project.id))
    val relay = nodes.map { element =>
      val id    = element.identifier.id
      val model = project.schema.getModelByName_!(element.identifier.typeName)
      val x     = relayIds += ProjectRelayId(id = id, stableModelIdentifier = model.stableIdentifier)
      x.asTry
    }
    DBIO.sequence(items ++ relay)
  }

  private def generateImportRelationsDBActions(relations: Vector[ImportRelation]): DBIOAction[Vector[Try[Int]], NoStream, Effect.Write] = {
    val x = relations.map { element =>
      val (left, right) = (element.left, element.right) match {
        case (l, r) if l.fieldName.isDefined => (l, r)
        case (l, r) if r.fieldName.isDefined => (r, l)
        case _                               => throw sys.error("Invalid ImportRelation at least one fieldName needs to be defined.")
      }

      val fromModel                                                 = project.schema.getModelByName_!(left.identifier.typeName)
      val fromField                                                 = fromModel.getFieldByName_!(left.fieldName.get)
      val relationSide: com.prisma.shared.models.RelationSide.Value = fromField.relationSide.get
      val relation: Relation                                        = fromField.relation.get

      val aValue: String = if (relationSide == RelationSide.A) left.identifier.id else right.identifier.id
      val bValue: String = if (relationSide == RelationSide.A) right.identifier.id else left.identifier.id
      // the empty list is for the RelationFieldMirrors
      DatabaseMutationBuilder.createRelationRow(project.id, relation.id, Cuid.createCuid(), aValue, bValue).asTry
    }
    DBIO.sequence(x)
  }

  private def generateImportListsDBActions(lists: Vector[ImportList]): DBIOAction[Vector[Try[Int]], NoStream, jdbc.MySQLProfile.api.Effect] = {
    val updateListValueActions = lists.flatMap { element =>
      def isDateTime(fieldName: String) =
        project.schema.getModelByName_!(element.identifier.typeName).getFieldByName_!(fieldName).typeIdentifier == TypeIdentifier.DateTime
      def isJson(fieldName: String) =
        project.schema.getModelByName_!(element.identifier.typeName).getFieldByName_!(fieldName).typeIdentifier == TypeIdentifier.Json

      element.values.map {
        case (fieldName, values) if isDateTime(fieldName) =>
          DatabaseMutationBuilder
            .pushScalarList(project.id, element.identifier.typeName, fieldName, element.identifier.id, values.map(dateTimeFromISO8601))
            .asTry
        case (fieldName, values) if isJson(fieldName) =>
          DatabaseMutationBuilder
            .pushScalarList(project.id, element.identifier.typeName, fieldName, element.identifier.id, values.map(v => v.toJson))
            .asTry
        case (fieldName, values) =>
          DatabaseMutationBuilder.pushScalarList(project.id, element.identifier.typeName, fieldName, element.identifier.id, values).asTry
      }
    }
    DBIO.sequence(updateListValueActions)
  }

  private def runDBActions(actions: DBIOAction[Vector[Try[Int]], NoStream, Effect.Write]): Future[Vector[Try[Int]]] = db.master.run(actions)
}
