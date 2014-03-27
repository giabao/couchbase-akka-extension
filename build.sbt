organization := "com.sandinh"

name := "couchbase-akka-extension"

version := "2.1.4"

scalaVersion := "2.10.4"

crossScalaVersions := Seq(
//  "2.11.0-RC3",
  "2.10.4"
)

//see https://github.com/scala/scala/blob/2.10.x/src/compiler/scala/tools/nsc/settings/ScalaSettings.scala
scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", //"-optimise",
  "-Xmigration", //"–Xverify", "-Xcheck-null", "-Ystatistics",
  "-Yinline-warnings", //"-Yinline",
  "-Ywarn-dead-code", "-Ydead-code"
)

javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "org.specs2"                %% "specs2"             % "2.3.10"  % "test",
    "com.typesafe.play"         %% "play-json"          % "2.2.2"   % "optional",
    "com.typesafe.akka"         %% "akka-actor"         % "2.2.4",
    "net.spy"                   %  "spymemcached"       % "2.10.6",
    "com.couchbase.client"      %  "couchbase-client"   % "1.3.2"
)

xerial.sbt.Sonatype.sonatypeSettings

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := <url>https://github.com/giabao/couchbase-akka-extension</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:giabao/couchbase-akka-extension.git</url>
    <connection>scm:git:git@github.com:giabao/couchbase-akka-extension.git</connection>
  </scm>
  <developers>
    <developer>
      <id>giabao</id>
      <name>Gia Bảo</name>
      <email>giabao@sandinh.net</email>
      <organization>Sân Đình</organization>
      <organizationUrl>http://sandinh.com</organizationUrl>
    </developer>
  </developers>
