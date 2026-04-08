package com.creditflow.transactions

import com.creditflow.shared.KafkaTopics
import com.creditflow.shared.events.{EventJson, TransactionCreatedEvent}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties

class TransactionKafkaProducer(bootstrapServers: String, clientId: String) {

  private val props = new Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("client.id", clientId)
  props.put("key.serializer", classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)
  props.put("acks", "1") // leader ack only; ok for dev

  private val producer = new KafkaProducer[String, String](props)

  def sendEvent(event: TransactionCreatedEvent): Unit = {
    val json = EventJson.toJsonString(event)
    // partition by user id so same user stays ordered on one partition (single partition topic anyway)
    val rec = new ProducerRecord[String, String](KafkaTopics.TransactionEvents, event.userId, json)
    producer.send(rec)
    producer.flush()
  }

  def close(): Unit = producer.close()
}
