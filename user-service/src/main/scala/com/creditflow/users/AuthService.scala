package com.creditflow.users

import at.favre.lib.crypto.bcrypt.BCrypt
import io.circe.Json
import io.circe.syntax._
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.time.Instant
import java.util.UUID

final case class JwtPayload(userId: UUID, email: String)

class AuthService(
    jwtSecret: String,
    jwtTtlSeconds: Long
) {

  private val jwtAlgorithms = Seq(JwtAlgorithm.HS256)

  def hashPassword(plain: String): String =
    BCrypt.withDefaults().hashToString(12, plain.toCharArray) // cost 12 = reasonable default

  def verifyPassword(plain: String, hash: String): Boolean =
    BCrypt.verifyer().verify(plain.toCharArray, hash).verified

  def issueToken(user: User): String = {
    val now = Instant.now().getEpochSecond
    // claims must match what parseToken reads back
    val claim = JwtClaim(
      content = Json
        .obj(
          "userId" -> user.id.toString.asJson,
          "email"  -> user.email.asJson
        )
        .noSpaces,
      expiration = Some(now + jwtTtlSeconds),
      issuedAt = Some(now)
    )
    JwtCirce.encode(claim, jwtSecret, JwtAlgorithm.HS256)
  }

  def parseToken(token: String): Either[String, JwtPayload] =
    JwtCirce.decodeJson(token, jwtSecret, jwtAlgorithms).toEither.left.map(_.getMessage).flatMap { json =>
      val c = json.hcursor
      for {
        uid <- c.get[String]("userId").left.map(_.message)
        em  <- c.get[String]("email").left.map(_.message)
        id  <- scala.util.Try(UUID.fromString(uid)).toEither.left.map(_.getMessage)
      } yield JwtPayload(id, em)
    }
}
