package com.creditflow.shared.events

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import java.time.Instant

// shared contract: producer and consumer must agree on field names (type is json key)
final case class TransactionCreatedEvent(
    userId: String,
    amount: BigDecimal,
    `type`: String,
    timestamp: String
)

object TransactionCreatedEvent {
  implicit val encoder: Encoder[TransactionCreatedEvent] = deriveEncoder[TransactionCreatedEvent]
  implicit val decoder: Decoder[TransactionCreatedEvent] = deriveDecoder[TransactionCreatedEvent]

  def now(userId: String, amount: BigDecimal, txnType: String): TransactionCreatedEvent =
    TransactionCreatedEvent(
      userId = userId,
      amount = amount,
      `type` = txnType,
      timestamp = Instant.now().toString
    )
}
