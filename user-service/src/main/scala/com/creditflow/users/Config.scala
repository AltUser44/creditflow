package com.creditflow.users

import com.typesafe.config.ConfigFactory

final case class UserServiceConfig(
    host: String,
    port: Int,
    jdbcUrl: String,
    dbUser: String,
    dbPassword: String,
    poolSize: Int,
    jwtSecret: String,
    jwtTtlSeconds: Long
)

object UserServiceConfig {
  // env wins over application.conf (handy for docker/local)
  def load(): UserServiceConfig = {
    val c = ConfigFactory.load().getConfig("creditflow")
    val db = c.getConfig("db")
    val jwt = c.getConfig("jwt")
    val http = c.getConfig("http")
    UserServiceConfig(
      host = Option(System.getenv("HTTP_HOST")).getOrElse(http.getString("host")),
      port = Option(System.getenv("HTTP_PORT")).map(_.toInt).getOrElse(http.getInt("port")),
      jdbcUrl = Option(System.getenv("JDBC_URL")).getOrElse(db.getString("url")),
      dbUser = Option(System.getenv("DB_USER")).getOrElse(db.getString("user")),
      dbPassword = Option(System.getenv("DB_PASSWORD")).getOrElse(db.getString("password")),
      poolSize = Option(System.getenv("DB_POOL_SIZE")).map(_.toInt).getOrElse(db.getInt("pool-size")),
      jwtSecret = Option(System.getenv("JWT_SECRET")).getOrElse(jwt.getString("secret")),
      jwtTtlSeconds = Option(System.getenv("JWT_TTL_SECONDS")).map(_.toLong).getOrElse(jwt.getLong("ttl-seconds"))
    )
  }
}
