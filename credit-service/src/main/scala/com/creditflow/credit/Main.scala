package com.creditflow.credit

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor

import scala.concurrent.Await
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    val cfg = CreditServiceConfig.load()

    implicit val system: ActorSystem = ActorSystem("credit-service")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val log = Logging(system, "Main")

    val xa = Transactor.fromDriverManager[IO]( // same pattern as other services
      "org.postgresql.Driver",
      cfg.jdbcUrl,
      new java.util.Properties() {
        put("user", cfg.dbUser)
        put("password", cfg.dbPassword)
      },
      None
    )

    val repo = new CreditRepository(xa)
    val routes = new CreditRoutes(repo)

    // background thread polls kafka; http runs on akka dispatcher
    val kafka = new TransactionEventsConsumer(
      cfg.kafkaBootstrap,
      cfg.kafkaGroupId,
      repo,
      log
    )
    kafka.start()

    val bindingFuture = Http().newServerAt(cfg.host, cfg.port).bind(routes.routes)

    bindingFuture.onComplete {
      case Success(b) =>
        log.info(s"credit-service listening on ${b.localAddress}")
      case Failure(ex) =>
        log.error(ex, "Failed to bind HTTP")
        kafka.shutdown()
        system.terminate()
    }(ec)

    sys.addShutdownHook {
      kafka.shutdown()
      bindingFuture.flatMap(_.unbind())(ec).onComplete(_ => system.terminate())(ec)
    }

    // keep process alive until ctrl+c / terminate
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
