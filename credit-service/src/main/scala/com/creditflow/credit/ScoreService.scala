package com.creditflow.credit

import com.creditflow.shared.events.TransactionCreatedEvent

object ScoreService {

  // txnCount24h = count in last 24h including current event (already inserted)
  def computeDelta(event: TransactionCreatedEvent, txnCount24h: Int): Int = {
    val amount = event.amount.abs
    val t      = event.`type`.toLowerCase
    var d      = 0
    if (t == "debit" && amount > 1000) d -= 10
    if (t == "credit") d += 5
    if (txnCount24h >= 3) d += 2
    d
  }

  def clampScore(score: Int): Int = math.max(0, math.min(1000, score))
}
