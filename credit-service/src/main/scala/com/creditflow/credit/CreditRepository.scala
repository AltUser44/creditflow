package com.creditflow.credit

import cats.effect.IO
import cats.implicits._
import com.creditflow.shared.events.TransactionCreatedEvent
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._ // instant in sql interpolator

import java.time.Instant

class CreditRepository(xa: Transactor[IO]) {

  def processTransactionEvent(event: TransactionCreatedEvent): IO[Unit] = {
    val occurredAt = Instant.parse(event.timestamp)
    // one db transaction: log event, count window, read old score, write new score
    val program = for {
      _   <- insertScoreEvent(event.userId, occurredAt)
      cnt <- countTxns24h(event.userId) // includes row we just inserted
      prev <- findScore(event.userId)
      base = prev.getOrElse(600)
      delta = ScoreService.computeDelta(event, cnt.toInt)
      newScore = ScoreService.clampScore(base + delta)
      _ <- upsertScore(event.userId, newScore)
    } yield ()
    program.transact(xa)
  }

  def getScore(userId: String): IO[Option[Int]] =
    sql"""SELECT score FROM credit_scores WHERE user_id = $userId"""
      .query[Int]
      .option
      .transact(xa)

  private def insertScoreEvent(userId: String, at: Instant): ConnectionIO[Int] =
    sql"""INSERT INTO score_events (user_id, occurred_at) VALUES ($userId, $at)""".update.run

  private def countTxns24h(userId: String): ConnectionIO[Long] =
    sql"""SELECT COUNT(*) FROM score_events WHERE user_id = $userId AND occurred_at >= NOW() - INTERVAL '24 hours'"""
      .query[Long]
      .unique

  private def findScore(userId: String): ConnectionIO[Option[Int]] =
    sql"""SELECT score FROM credit_scores WHERE user_id = $userId"""
      .query[Int]
      .option

  private def upsertScore(userId: String, score: Int): ConnectionIO[Int] =
    sql"""INSERT INTO credit_scores (user_id, score, updated_at) VALUES ($userId, $score, NOW())
          ON CONFLICT (user_id) DO UPDATE SET score = EXCLUDED.score, updated_at = NOW()""".update.run
}
