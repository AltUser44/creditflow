package com.creditflow.transactions

import com.typesafe.config.ConfigFactory

final case class TransactionServiceConfig(
    host: String,
    port: Int,
    jdbcUrl: String,
    dbUser: String,
    dbPassword: String,
    kafkaBootstrap: String,
    kafkaClientId: String
)

object TransactionServiceConfig {
  // env overrides for docker vs laptop
  def load(): TransactionServiceConfig = {
    val c = ConfigFactory.load().getConfig("creditflow")
    val db = c.getConfig("db")
    val k = c.getConfig("kafka")
    val http = c.getConfig("http")
    TransactionServiceConfig(
      host = Option(System.getenv("HTTP_HOST")).getOrElse(http.getString("host")),
      port = Option(System.getenv("HTTP_PORT")).map(_.toInt).getOrElse(http.getInt("port")),
      jdbcUrl = Option(System.getenv("JDBC_URL")).getOrElse(db.getString("url")),
      dbUser = Option(System.getenv("DB_USER")).getOrElse(db.getString("user")),
      dbPassword = Option(System.getenv("DB_PASSWORD")).getOrElse(db.getString("password")),
      kafkaBootstrap = Option(System.getenv("KAFKA_BOOTSTRAP_SERVERS")).getOrElse(k.getString("bootstrap-servers")),
      kafkaClientId = Option(System.getenv("KAFKA_CLIENT_ID")).getOrElse(k.getString("client-id"))
    )
  }
}
