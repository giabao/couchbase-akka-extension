organization := "com.sandinh"

name := "couchbase-akka-extension"

version := "3.1.0"

scalaVersion := "2.11.0"

crossScalaVersions := Seq(
  "2.11.0",
  "2.10.4"
)

//see https://github.com/scala/scala/blob/2.10.x/src/compiler/scala/tools/nsc/settings/ScalaSettings.scala
scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", //"-optimise",
  "-Xmigration", "-Xfuture", //"–Xverify", "-Xcheck-null", "-Ystatistics",
  "-Yinline-warnings", //"-Yinline",
  "-Ywarn-dead-code", "-Ydead-code"
)

javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "org.specs2"                %% "specs2"             % "2.3.11"    % "test",
    "com.typesafe.play"         %% "play-json"          % "2.3.0-RC1" % "optional",
    "com.typesafe.akka"         %% "akka-actor"         % "2.3.2",
    //note: couchbase-client depends on spymemcached 2.11.1 which has some critical bugs.
    //we must manually compile & deploy spy 2.11.1-1 or wait for spy 2.11.2 (& couchbase-client 1.4.1)
    "com.couchbase.client"      %  "couchbase-client"   % "1.4.0"
)
