package com.creditflow.users

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
    val cfg = UserServiceConfig.load()

    implicit val system: ActorSystem = ActorSystem("user-service")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val log = Logging(system, "Main")

    // doobie 1.x wants props + optional log handler, not raw user/pass args
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      cfg.jdbcUrl,
      new java.util.Properties() {
        put("user", cfg.dbUser)
        put("password", cfg.dbPassword)
      },
      None
    )

    val repo = new UserRepository(xa)
    val auth = new AuthService(cfg.jwtSecret, cfg.jwtTtlSeconds)
    val routes = new UserRoutes(repo, auth)

    val bindingFuture = Http().newServerAt(cfg.host, cfg.port).bind(routes.routes)

    bindingFuture.onComplete {
      case Success(b) =>
        log.info(s"user-service listening on ${b.localAddress}")
      case Failure(ex) =>
        log.error(ex, "Failed to bind HTTP")
        system.terminate()
    }(ec)

    sys.addShutdownHook {
      bindingFuture.flatMap(_.unbind())(ec).onComplete(_ => system.terminate())(ec)
    }

    // block so the jvm doesn't exit right after bind (akka alone won't keep process up)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
