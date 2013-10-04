organization := "sandinh"

name := "couchbase-akka-extension"

version := "1.0.0"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", "-Yinline-warnings"/*, "-optimise"*/)

javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")

libraryDependencies ++= Seq(
    "com.typesafe.akka"         %% "akka-actor"         % "2.2.1",
    "com.couchbase.client"      %  "couchbase-client"   % "1.2.0"
)
