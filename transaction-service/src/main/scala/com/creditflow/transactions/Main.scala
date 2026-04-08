package com.creditflow.transactions

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
    val cfg = TransactionServiceConfig.load()

    implicit val system: ActorSystem = ActorSystem("transaction-service")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val log = Logging(system, "Main")

    // see user-service Main for why props + none
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      cfg.jdbcUrl,
      new java.util.Properties() {
        put("user", cfg.dbUser)
        put("password", cfg.dbPassword)
      },
      None
    )

    val repo = new TransactionRepository(xa)
    val kafka = new TransactionKafkaProducer(cfg.kafkaBootstrap, cfg.kafkaClientId)
    val routes = new TransactionRoutes(repo, kafka)

    val bindingFuture = Http().newServerAt(cfg.host, cfg.port).bind(routes.routes)

    bindingFuture.onComplete {
      case Success(b) =>
        log.info(s"transaction-service listening on ${b.localAddress}")
      case Failure(ex) =>
        log.error(ex, "Failed to bind HTTP")
        system.terminate()
    }(ec)

    sys.addShutdownHook {
      kafka.close() // flush pending sends on shutdown
      bindingFuture.flatMap(_.unbind())(ec).onComplete(_ => system.terminate())(ec)
    }

    Await.result(system.whenTerminated, Duration.Inf) // don't exit after bind
  }
}
