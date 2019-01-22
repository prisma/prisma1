package com.prisma.deploy.connector.mongo.database

import com.prisma.deploy.connector.mongo.impl.DeployMongoAction
import com.prisma.shared.models.{Model, Project}
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Sorts.ascending
import org.mongodb.scala.{MongoNamespace, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NoAction {
  val unit = {
    DeployMongoAction { database =>
      Future.successful(())
    }
  }
}

object MongoDeployDatabaseMutationBuilder {

  //Project
  def createClientDatabaseForProject = DeployMongoAction { database =>
    database.listCollectionNames().toFuture().map(_ => ())
  }

  //Fixme Should this be allowed to delete collections? probably not, should just drop contents
  def truncateProjectTables(project: Project) = DeployMongoAction { database =>
    val nonEmbeddedModels = project.models.filter(!_.isEmbedded)

    def dropTables: Future[List[Completed]] = Future.sequence(nonEmbeddedModels.map(model => database.getCollection(model.dbName).drop().toFuture()))

    def createTables(drops: List[Completed]): Future[List[Completed]] =
      Future.sequence(nonEmbeddedModels.map(model => database.createCollection(model.dbName).toFuture()))

    def createUniques(creates: List[Completed]): Future[List[List[Any]]] =
      Future.sequence(nonEmbeddedModels.map { model =>
        Future.sequence(model.scalarNonListFields.filter(_.isUnique).map(field => addUniqueConstraint(database, model.dbName, field.name)))
      })

    def createRelationIndexes(uniques: List[List[Any]]) =
      Future.sequence(project.relations.collect {
        case relation if relation.isInlineRelation =>
          relation.modelAField.relationIsInlinedInParent match {
            case true if !relation.modelB.isEmbedded  => addRelationIndex(database, relation.modelAField.model.dbName, relation.modelAField.dbName)
            case false if !relation.modelA.isEmbedded => addRelationIndex(database, relation.modelBField.model.dbName, relation.modelBField.dbName)
          }
      })

    for {
      drops: List[Completed]   <- dropTables
      creates: List[Completed] <- createTables(drops)
      uniques                  <- createUniques(creates)
      relations                <- createRelationIndexes(uniques)
    } yield ()
  }

  def nonDestructiveTruncateProjectTables(project: Project) = DeployMongoAction { database =>
    val nonEmbeddedModels = project.models.filter(!_.isEmbedded)

    for {
      _ <- Future.sequence(nonEmbeddedModels.map(model => database.getCollection(model.dbName).deleteMany(Document().toBsonDocument).toFuture()))
    } yield ()
  }

  def deleteProjectDatabase = DeployMongoAction { database =>
    Future.successful(())
//    database.drop().toFuture().map(_ -> Unit)
  }

  //Collection
  def createCollection(collectionName: String) = DeployMongoAction { database =>
    database.listCollectionNames().toFuture().map { names =>
      if (names.contains(collectionName)) {
        Future.successful(())
      } else {
        database.createCollection(collectionName).toFuture().map(_ -> Unit)
      }
    }
  }

  def dropCollection(collectionName: String) = DeployMongoAction { database =>
    Future.successful(())
//    database.getCollection(collectionName).drop().toFuture().map(_ -> Unit)
  }

  def renameCollection(project: Project, collectionName: String, newName: String) = DeployMongoAction { database =>
    Future.successful(())

//    database.getCollection(collectionName).renameCollection(MongoNamespace(projectId, newName)).toFuture().map(_ -> Unit)
  }

  //Fields
  def createField(model: Model, fieldName: String) = DeployMongoAction { database =>
    model.isEmbedded match {
      case false => addUniqueConstraint(database, model.dbName, fieldName)
      case true  => Future.successful(())
    }
  }

  def deleteField(model: Model, fieldName: String) = DeployMongoAction { database =>
    model.isEmbedded match {
      case false => removeUniqueConstraint(database, model.dbName, fieldName)
      case true  => Future.successful(())
    }
  }

  def updateField(name: String, newName: String) = NoAction.unit

  //Fixme once Mongo fixes this bug we can implement nested Unique Constraints properly https://jira.mongodb.org/browse/SERVER-1068
//  def addNestedUniqueConstraint(database: MongoDatabase, model: Model, fieldName: String) = {
//
//    val fieldToParent = model.relationFields.find(rf => rf.isHidden && rf.relatedField.isVisible).get
//    val topModel      = topLevelModel(fieldToParent)
//    val fieldNames    = (fieldName +: recurse(fieldToParent)).reverse
//
//    println(fieldNames)
//    addEmbeddedUniqueConstraint(database, topModel.dbName, recurse2(fieldNames).reverse)
//  }
//
//  def topLevelModel(relationField: RelationField): Model = relationField.model.isEmbedded match {
//    case true  => topLevelModel(relationField.relatedField)
//    case false => relationField.model
//  }
//
//  def recurse2(strings: Vector[String]): Vector[String] = strings match {
//    case one if one.length == 1  => one
//    case many if many.length > 1 => Vector(many.mkString(".")) ++ recurse2(strings.dropRight(1))
//  }
//
//  def recurse(relationField: RelationField): Vector[String] = relationField.model.isEmbedded match {
//    case true  => relationField.relatedField.name +: recurse(relationField.relatedField)
//    case false => Vector.empty
//  }
//
//  def addEmbeddedUniqueConstraint(database: MongoDatabase, collectionName: String, fieldNames: Vector[String]) = {
////    val shortenedName = indexNameHelper(collectionName, fieldName, true)
//    val shortenedName = "compoundindex"
//    val doc           = Document(fieldNames.map(x => x -> 1))
//
//    database
//      .getCollection(collectionName)
//      .createIndex(doc, IndexOptions().unique(true).sparse(true).name(shortenedName))
//      .toFuture()
//      .map(_ => ())
//  }

  def addUniqueConstraint(database: MongoDatabase, collectionName: String, fieldName: String) = {
    val shortenedName = indexNameHelper(collectionName, fieldName, true)

    database
      .getCollection(collectionName)
      .createIndex(ascending(fieldName), IndexOptions().unique(true).sparse(true).name(shortenedName))
      .toFuture()
      .map(_ => ())
  }

  def removeUniqueConstraint(database: MongoDatabase, collectionName: String, fieldName: String) = {
    val shortenedName = indexNameHelper(collectionName, fieldName, true)

    database
      .getCollection(collectionName)
      .dropIndex(shortenedName)
      .toFuture()
      .map(_ => ())
  }

  def addRelationIndex(database: MongoDatabase, collectionName: String, fieldName: String) = {
    val shortenedName = indexNameHelper(collectionName, fieldName, false)

    database
      .getCollection(collectionName)
      .createIndex(ascending(fieldName), IndexOptions().name(shortenedName))
      .toFuture()
      .map(_ => ())
  }

  def removeRelationIndex(database: MongoDatabase, collectionName: String, fieldName: String) = {
    val shortenedName = indexNameHelper(collectionName, fieldName, false)

    database
      .getCollection(collectionName)
      .dropIndex(shortenedName)
      .toFuture()
      .map(_ => ())
  }

  def indexNameHelper(collectionName: String, fieldName: String, unique: Boolean): String = {
    // TODO: explain this magic calculation
    val shortenedName = fieldName.replaceAll("_", "x") substring (0, (125 - 25 - collectionName.length - 12).min(fieldName.length))

    unique match {
      case false => shortenedName + "_R"
      case true  => shortenedName + "_U"
    }
  }

}
