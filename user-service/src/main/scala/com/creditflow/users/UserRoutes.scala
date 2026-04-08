package com.creditflow.users

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, Directives, Route}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.postgresql.util.PSQLException

import java.util.UUID
import scala.util.{Failure, Success}

final case class RegisterRequest(email: String, password: String)
final case class LoginRequest(email: String, password: String)
final case class LoginResponse(token: String, tokenType: String)
final case class RegisterResponse(id: String, email: String)
final case class MeResponse(id: String, email: String)
final case class ErrorBody(message: String)

class UserRoutes(
    repo: UserRepository,
    auth: AuthService
) extends Directives {

  // bearer token only; missing/invalid -> 401 via rejection handler
  private def jwtUserId: Directive1[UUID] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(h) if h.startsWith("Bearer ") =>
        val token = h.stripPrefix("Bearer ").trim
        auth.parseToken(token) match {
          case Right(p) => provide(p.userId)
          case Left(_)  => reject(AuthorizationFailedRejection)
        }
      case _ => reject(AuthorizationFailedRejection)
    }

  val routes: Route =
    pathPrefix("users") {
      post {
        path("register") {
          entity(as[RegisterRequest]) { req =>
            // doobie runs on cats io; bridge to future for akka
            onComplete(repo.insert(req.email, auth.hashPassword(req.password)).unsafeToFuture()) {
              case Success(id) =>
                complete(StatusCodes.Created, RegisterResponse(id.toString, req.email))
              case Failure(e: PSQLException) if e.getSQLState == "23505" => // unique violation
                complete(StatusCodes.Conflict, ErrorBody("Email already registered"))
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, ErrorBody(ex.getMessage))
            }
          }
        } ~
          path("login") {
            entity(as[LoginRequest]) { req =>
              val io: IO[Either[String, User]] = for {
                opt <- repo.findByEmail(req.email)
              } yield opt match {
                case Some(u) if auth.verifyPassword(req.password, u.passwordHash) => Right(u)
                case _                                                            => Left("Invalid credentials")
              }
              onSuccess(io.unsafeToFuture()) {
                case Right(u) =>
                  complete(LoginResponse(auth.issueToken(u), "Bearer"))
                case Left(msg) =>
                  complete(StatusCodes.Unauthorized, ErrorBody(msg))
              }
            }
          }
      } ~
        get {
          path("me") {
            jwtUserId { uid =>
              onSuccess(repo.findById(uid).unsafeToFuture()) {
                case Some(u) =>
                  complete(MeResponse(u.id.toString, u.email))
                case None =>
                  complete(StatusCodes.NotFound, ErrorBody("User not found"))
              }
            }
          }
        }
    }
}
