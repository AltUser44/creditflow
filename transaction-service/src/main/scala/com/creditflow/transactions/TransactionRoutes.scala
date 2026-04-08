package com.creditflow.transactions

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import cats.effect.unsafe.implicits.global
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.util.{Failure, Success}

final case class CreateTransactionRequest(userId: String, amount: BigDecimal, `type`: String)
final case class CreateTransactionResponse(id: String, userId: String, amount: BigDecimal, `type`: String)
final case class ErrorBody(message: String)

class TransactionRoutes(
    repo: TransactionRepository,
    kafka: TransactionKafkaProducer
) {

  private val allowedTypes = Set("debit", "credit")

  val routes =
    path("transactions") {
      post {
        entity(as[CreateTransactionRequest]) { req =>
          val t = req.`type`.toLowerCase // normalize debit/credit
          if (!allowedTypes.contains(t)) {
            complete(StatusCodes.BadRequest, ErrorBody("type must be debit or credit"))
          } else {
            // event mirrors what credit-service expects (iso timestamp in json)
            val event =
              com.creditflow.shared.events.TransactionCreatedEvent.now(req.userId, req.amount.abs, t)
            onComplete(repo.insert(req.userId, req.amount.abs, t).unsafeToFuture()) {
              case Success(id) =>
                kafka.sendEvent(event) // after db commit in practice you'd use outbox; here simple path
                complete(
                  StatusCodes.Created,
                  CreateTransactionResponse(id.toString, req.userId, req.amount.abs, t)
                )
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, ErrorBody(ex.getMessage))
            }
          }
        }
      }
    }
}
