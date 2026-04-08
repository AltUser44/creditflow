package com.creditflow.shared.events

import io.circe.parser.decode
import io.circe.syntax._

object EventJson {
  // single place for wire format so services don't drift
  def toJsonString(event: TransactionCreatedEvent): String =
    event.asJson.noSpaces

  def parseTransactionCreated(json: String): Either[io.circe.Error, TransactionCreatedEvent] =
    decode[TransactionCreatedEvent](json)
}
