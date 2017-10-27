package cool.graph.client.mutations

import cool.graph.client.authorization.PermissionQueryArg
import cool.graph.client.mutations.definitions.UpdateDefinition
import cool.graph.shared.models._
import cool.graph.util.coolSangria.Sangria
import cool.graph.{ArgumentSchema, ClientMutationDefinition, CreateOrUpdateMutationDefinition, DataItem}

import scala.collection.immutable.Seq

/**
  * It's called CoolArgs to easily differentiate from Sangrias Args class.
  */
case class CoolArgs(raw: Map[String, Any], argumentSchema: ArgumentSchema, model: Model, project: Project) {
  private val sangriaArgs = Sangria.rawArgs(raw)

  def subArgsList(field: Field): Option[Seq[CoolArgs]] = {
    val subModel = field.relatedModel(project).get
    val fieldValues: Option[Seq[Map[String, Any]]] = field.isList match {
      case true  => getFieldValuesAs[Map[String, Any]](field)
      case false => getFieldValueAsSeq[Map[String, Any]](field.name)
    }

    fieldValues match {
      case None    => None
      case Some(x) => Some(x.map(CoolArgs(_, argumentSchema, subModel, project)))
    }
  }

  def hasArgFor(field: Field) = raw.get(field.name).isDefined

  def fields: Seq[Field] = {
    for {
      field <- model.fields
      if hasArgFor(field)
    } yield field
  }

  def fieldsThatRequirePermissionCheckingInMutations = {
    fields.filter(_.name != "id")
  }

  /**
    * The outer option is defined if the field key was specified in the arguments at all.
    * The inner option is empty if a null value was sent for this field. If the option is defined it contains a non null value
    * for this field.
    */
  def getFieldValueAs[T](field: Field, suffix: String = ""): Option[Option[T]] = {
    getFieldValueAs(field.name + suffix)
  }

  def getFieldValueAs[T](name: String): Option[Option[T]] = {
    raw.get(name).map { fieldValue =>
      try {
        fieldValue.asInstanceOf[Option[T]]
      } catch {
        case _: ClassCastException =>
          Option(fieldValue.asInstanceOf[T])
      }
    }
  }

  def getFieldValueAsSeq[T](name: String): Option[Seq[T]] = {
    raw.get(name).map { fieldValue =>
      try {
        fieldValue.asInstanceOf[Option[T]] match {
          case Some(x) => Seq(x)
          case None    => Seq.empty

        }
      } catch {
        case _: ClassCastException =>
          Seq(fieldValue.asInstanceOf[T])
      }
    }
  }

  /**
    * The outer option is defined if the field key was specified in the arguments at all.
    * The inner sequence then contains all the values specified.
    */
  def getFieldValuesAs[T](field: Field, suffix: String = ""): Option[Seq[T]] = {
    raw.get(field.name + suffix).map { fieldValue =>
      try {
        fieldValue.asInstanceOf[Option[Seq[T]]].getOrElse(Seq.empty)
      } catch {
        case _: ClassCastException =>
          fieldValue.asInstanceOf[Seq[T]]
      }
    }
  }

  def permissionQueryArgsForNewAndOldFieldValues(updateDefinition: UpdateDefinition, existingNode: Option[DataItem]): List[PermissionQueryArg] = {
    val thePermissionQueryArgsForNewFieldValues = permissionQueryArgsForNewFieldValues(updateDefinition)

    val permissionQueryArgsForOldFieldValues = existingNode match {
      case Some(existingNode) =>
        model.scalarFields.flatMap { field =>
          List(
            PermissionQueryArg(s"$$old_${field.name}", existingNode.getOption(field.name).getOrElse(""), field.typeIdentifier),
            PermissionQueryArg(s"$$node_${field.name}", existingNode.getOption(field.name).getOrElse(""), field.typeIdentifier)
          )
        }
      case None =>
        List.empty
    }

    thePermissionQueryArgsForNewFieldValues ++ permissionQueryArgsForOldFieldValues
  }

  def permissionQueryArgsForNewFieldValues(mutationDefinition: CreateOrUpdateMutationDefinition): List[PermissionQueryArg] = {
    val scalarArgumentValues = argumentSchema.extractArgumentValues(sangriaArgs, mutationDefinition.getScalarArguments(model))

    val scalarPermissionQueryArgs = scalarArgumentValues.flatMap { argumentValue =>
      List(
        PermissionQueryArg(s"$$new_${argumentValue.field.get.name}", argumentValue.value, argumentValue.field.get.typeIdentifier),
        PermissionQueryArg(s"$$input_${argumentValue.field.get.name}", argumentValue.value, argumentValue.field.get.typeIdentifier)
      )
    }

    val relationalArgumentValues = argumentSchema.extractArgumentValues(sangriaArgs, mutationDefinition.getRelationArguments(model))

    val singleRelationPermissionQueryArgs: Seq[PermissionQueryArg] = relationalArgumentValues.flatMap { argumentValue =>
      List(
        PermissionQueryArg(s"$$new_${argumentValue.field.get.name}Id", argumentValue.value, TypeIdentifier.GraphQLID),
        PermissionQueryArg(s"$$input_${argumentValue.field.get.name}Id", argumentValue.value, TypeIdentifier.GraphQLID)
      )
    }

    scalarPermissionQueryArgs ++ singleRelationPermissionQueryArgs
  }
}
