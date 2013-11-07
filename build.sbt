organization := "com.sandinh"

name := "couchbase-akka-extension"

version := "2.0.5"

scalaVersion := "2.10.3"

//see https://github.com/scala/scala/blob/2.10.x/src/compiler/scala/tools/nsc/settings/ScalaSettings.scala
scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", //"-optimise",
  "-Xmigration", //"–Xverify", "-Xcheck-null", "-Ystatistics",
  "-Yinline-warnings", "-Ywarn-dead-code", "-Yinline", "-Ydead-code"
)

javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "org.specs2"                %% "specs2"             % "2.3.3"   % "test",
    "com.typesafe.play"         %% "play-json"          % "2.2.1"   % "optional",
    "com.typesafe.akka"         %% "akka-actor"         % "2.2.3",
    "com.couchbase.client"      %  "couchbase-client"   % "1.2.2"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/giabao/couchbase-akka-extension</url>
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
  </developers>)
