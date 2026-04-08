package com.creditflow.credit

import akka.http.scaladsl.server.Directives._
import cats.effect.unsafe.implicits.global
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

final case class CreditScoreResponse(userId: String, score: Int)

class CreditRoutes(repo: CreditRepository) {

  val routes =
    pathPrefix("credit-score") {
      get {
        path(Segment) { userId =>
          onSuccess(repo.getScore(userId).unsafeToFuture()) {
            case Some(s) =>
              complete(CreditScoreResponse(userId, s))
            case None =>
              complete(CreditScoreResponse(userId, 600)) // no row yet = default baseline
          }
        }
      }
    }
}
