package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.models.Client
import cool.graph.system.externalServices.{Auth0Api, Auth0ApiUpdateValues}
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

case class UpdateCustomerInAuth0(oldClient: Client, client: Client)(implicit inj: Injector) extends Mutaction with Injectable {
  override def execute: Future[MutactionExecutionSuccess] = {
    val emailUpdate = oldClient.email == client.email match {
      case true  => None
      case false => Some(client.email)
    }

    emailUpdate match {
      case None =>
        Future.successful(MutactionExecutionSuccess())

      case Some(_) =>
        val values = Auth0ApiUpdateValues(email = emailUpdate)

        val auth0Api = inject[Auth0Api]

        auth0Api.updateClient(client.auth0Id.get, values).map {
          case true  => MutactionExecutionSuccess()
          case false => throw new Exception("Updating Auth0 failed")
        }
    }
  }

  override def rollback = Some(UpdateCustomerInAuth0(oldClient = client, client = oldClient).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    client.auth0Id match {
      case None    => throw new Exception(s"Client ${client.id} does not have a auth0Id")
      case Some(_) => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
