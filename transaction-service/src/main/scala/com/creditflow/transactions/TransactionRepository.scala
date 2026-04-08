package com.creditflow.transactions

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import java.util.UUID

class TransactionRepository(xa: Transactor[IO]) {

  // txn_type column: "type" is awkward in sql
  def insert(userId: String, amount: BigDecimal, txnType: String): IO[UUID] =
    sql"""INSERT INTO transactions (user_id, amount, txn_type) VALUES ($userId, $amount, $txnType) RETURNING id"""
      .query[UUID]
      .unique
      .transact(xa)
}
