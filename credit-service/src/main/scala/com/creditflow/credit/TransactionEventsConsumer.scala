package com.creditflow.credit

import akka.event.LoggingAdapter
import cats.effect.unsafe.implicits.global
import com.creditflow.shared.KafkaTopics
import com.creditflow.shared.events.EventJson
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import java.util.Properties
import scala.jdk.CollectionConverters._

class TransactionEventsConsumer(
    bootstrapServers: String,
    groupId: String,
    repo: CreditRepository,
    log: LoggingAdapter
) {

  private val props = new Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("group.id", groupId)
  props.put("key.deserializer", classOf[StringDeserializer].getName)
  props.put("value.deserializer", classOf[StringDeserializer].getName)
  props.put("auto.offset.reset", "earliest") // new group reads from start
  props.put("enable.auto.commit", "true") // demo simplicity; at-least-once

  private val consumer = new KafkaConsumer[String, String](props)

  @volatile private var running = true

  def start(): Unit = {
    consumer.subscribe(java.util.Collections.singletonList(KafkaTopics.TransactionEvents))
    new Thread(() => pollLoop(), "kafka-transaction-events").start() // don't block main
  }

  private def pollLoop(): Unit =
    try {
      while (running) {
        try {
          val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofMillis(500))
          records.iterator().asScala.foreach { rec =>
            EventJson.parseTransactionCreated(rec.value()) match {
              case Right(event) =>
                // sync run keeps thread model simple; errors logged not thrown to kafka
                try repo.processTransactionEvent(event).unsafeRunSync()
                catch {
                  case ex: Exception =>
                    log.error(ex, s"Failed to process event for user ${event.userId}")
                }
              case Left(err) =>
                log.warning(s"Bad event JSON: $err")
            }
          }
        } catch {
          case _: WakeupException if !running =>
            () // shutdown path
          case _: WakeupException =>
            () // spurious wakeup, keep polling
          case ex: Exception if running =>
            log.error(ex, "Kafka poll error")
        }
      }
    } finally {
      consumer.close()
    }

  def shutdown(): Unit = {
    running = false
    consumer.wakeup() // unblocks poll so thread can hit finally/close
  }
}
