organization := "com.sandinh"

name := "couchbase-akka-extension"

version := "3.1.6"

scalaVersion := "2.11.2"

crossScalaVersions := Seq(
  "2.11.2",
  "2.10.4"
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", //"-optimise",
  "-Xmigration", "-Xfuture", //"–Xverify", "-Xcheck-null", "-Ystatistics",
  "-Yinline-warnings", //"-Yinline",
  "-Ywarn-dead-code", "-Ydead-code"
)

javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "org.specs2"                %% "specs2"             % "2.4"       % "test",
    "com.typesafe.play"         %% "play-json"          % "2.3.2"     % "optional",
    "com.typesafe.akka"         %% "akka-actor"         % "2.3.4",
    "com.couchbase.client"      %  "couchbase-client"   % "1.4.4"
)
