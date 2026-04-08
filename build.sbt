import Dependencies._

ThisBuild / scalaVersion := Versions.scalaV
ThisBuild / organization := "com.creditflow"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val shared = (project in file("shared"))
  .settings(
    name := "shared",
    libraryDependencies ++= sharedDeps
  )

lazy val userService = (project in file("user-service"))
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "user-service",
    libraryDependencies ++= serviceCommon ++ Seq(
      jwtCirceLib,
      bcryptLib
    ),
    Compile / mainClass := Some("com.creditflow.users.Main")
  )

lazy val transactionService = (project in file("transaction-service"))
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "transaction-service",
    libraryDependencies ++= serviceCommon,
    Compile / mainClass := Some("com.creditflow.transactions.Main")
  )

lazy val creditService = (project in file("credit-service"))
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "credit-service",
    libraryDependencies ++= serviceCommon,
    Compile / mainClass := Some("com.creditflow.credit.Main")
  )

lazy val root = (project in file("."))
  .aggregate(shared, userService, transactionService, creditService)
  .settings(
    name := "creditflow-root",
    publish / skip := true
  )
