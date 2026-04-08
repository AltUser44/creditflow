package com.creditflow.users

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import java.util.UUID

class UserRepository(xa: Transactor[IO]) {

  // returns new row id from postgres
  def insert(email: String, passwordHash: String): IO[UUID] = {
    sql"""INSERT INTO users (email, password_hash) VALUES ($email, $passwordHash) RETURNING id"""
      .query[UUID]
      .unique
      .transact(xa)
  }

  def findByEmail(email: String): IO[Option[User]] =
    sql"""SELECT id, email, password_hash FROM users WHERE email = $email"""
      .query[User]
      .option
      .transact(xa)

  def findById(id: UUID): IO[Option[User]] =
    sql"""SELECT id, email, password_hash FROM users WHERE id = $id"""
      .query[User]
      .option
      .transact(xa)
}
