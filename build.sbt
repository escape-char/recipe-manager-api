name := """recipe-manager-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  evolutions,
  ws,
  "com.typesafe.play" %% "anorm" % "2.4.0",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "com.pauldijou" %% "jwt-play" % "0.12.1",
  "com.pauldijou" %% "jwt-core" % "0.12.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "org.mindrot" % "jbcrypt" % "0.3m"
)