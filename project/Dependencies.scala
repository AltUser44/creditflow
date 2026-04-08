import sbt._

object Versions {
  val scalaV   = "2.13.14"
  val akka     = "2.6.21"
  val akkaHttp = "10.2.10"
  val circe    = "0.14.6"
  val doobie   = "1.0.0-RC5"
  val jwtScala = "9.4.6"
  val kafka    = "3.6.2"
  val bcrypt   = "0.10.2"
  val postgres = "42.7.3"
}

object Dependencies {
  import Versions._

  val akkaHttpLib: ModuleID       = "com.typesafe.akka" %% "akka-http"        % akkaHttp
  val akkaHttpCirceLib: ModuleID  = "de.heikoseeberger" %% "akka-http-circe"  % "1.39.2"
  val akkaStreamLib: ModuleID     = "com.typesafe.akka" %% "akka-stream"      % akka
  val akkaSlf4jLib: ModuleID      = "com.typesafe.akka" %% "akka-slf4j"       % akka

  val circeCoreLib: ModuleID      = "io.circe" %% "circe-core"    % circe
  val circeGenericLib: ModuleID   = "io.circe" %% "circe-generic" % circe
  val circeParserLib: ModuleID    = "io.circe" %% "circe-parser"  % circe

  val doobieCoreLib: ModuleID     = "org.tpolecat" %% "doobie-core"     % doobie
  val doobieHikariLib: ModuleID   = "org.tpolecat" %% "doobie-hikari"   % doobie
  val doobiePostgresLib: ModuleID = "org.tpolecat" %% "doobie-postgres" % doobie

  val jwtCirceLib: ModuleID       = "com.github.jwt-scala" %% "jwt-circe" % jwtScala

  val kafkaClientsLib: ModuleID   = "org.apache.kafka" % "kafka-clients" % kafka

  val bcryptLib: ModuleID         = "at.favre.lib" % "bcrypt" % bcrypt

  val postgresLib: ModuleID       = "org.postgresql" % "postgresql" % postgres

  val configLib: ModuleID         = "com.typesafe" % "config" % "1.4.3"

  val logbackLib: ModuleID        = "ch.qos.logback" % "logback-classic" % "1.4.14"

  val catsEffectLib: ModuleID     = "org.typelevel" %% "cats-effect" % "3.5.4"

  val sharedDeps: Seq[ModuleID] = Seq(
    circeCoreLib,
    circeGenericLib,
    circeParserLib,
    kafkaClientsLib,
    configLib
  )

  val serviceCommon: Seq[ModuleID] = Seq(
    akkaHttpLib,
    akkaHttpCirceLib,
    akkaStreamLib,
    akkaSlf4jLib,
    doobieCoreLib,
    doobieHikariLib,
    doobiePostgresLib,
    postgresLib,
    configLib,
    logbackLib,
    catsEffectLib
  )
}
