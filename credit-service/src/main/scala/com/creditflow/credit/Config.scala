package com.creditflow.credit

import com.typesafe.config.ConfigFactory

final case class CreditServiceConfig(
    host: String,
    port: Int,
    jdbcUrl: String,
    dbUser: String,
    dbPassword: String,
    kafkaBootstrap: String,
    kafkaGroupId: String
)

object CreditServiceConfig {
  // KAFKA_GROUP_ID new value = fresh consumer group (reprocess from earliest if reset policy applies)
  def load(): CreditServiceConfig = {
    val c = ConfigFactory.load().getConfig("creditflow")
    val db = c.getConfig("db")
    val k = c.getConfig("kafka")
    val http = c.getConfig("http")
    CreditServiceConfig(
      host = Option(System.getenv("HTTP_HOST")).getOrElse(http.getString("host")),
      port = Option(System.getenv("HTTP_PORT")).map(_.toInt).getOrElse(http.getInt("port")),
      jdbcUrl = Option(System.getenv("JDBC_URL")).getOrElse(db.getString("url")),
      dbUser = Option(System.getenv("DB_USER")).getOrElse(db.getString("user")),
      dbPassword = Option(System.getenv("DB_PASSWORD")).getOrElse(db.getString("password")),
      kafkaBootstrap = Option(System.getenv("KAFKA_BOOTSTRAP_SERVERS")).getOrElse(k.getString("bootstrap-servers")),
      kafkaGroupId = Option(System.getenv("KAFKA_GROUP_ID")).getOrElse(k.getString("group-id"))
    )
  }
}
