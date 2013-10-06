package akka.contrib.couchbase

import akka.actor.{ExtendedActorSystem, ExtensionIdProvider, ExtensionId, Extension}
import com.couchbase.client.{CouchbaseConnectionFactory, CouchbaseClient}
import com.typesafe.config.Config
import java.net.URI
import collection.JavaConverters._

/** Sample usage:
  *
  * 1. Add to application.conf
  * {{{
  *   akka.contrib.couchbase.buckets = [
  *     { bucket=bk1
  *       password="some password"
  *       nodes="http://cb1.sandinh.com:8091/pools;http://cb2.sandinh.com:8091/pools"
  *     },
  *     { bucket=bk2
  *       password="some password"
  *       nodes="http://cb3.sandinh.com:8091/pools"
  *     }
  *   ]
  * }}}
  *
  * 2. Get CouchbaseClient from an ActorSystem.
  *
  * This example use Play framework 2 - in which, we can get ActorSystem by importing.
  * Of course, you can get CouchbaseClient without Play.
  * {{{
  *   import play.api.libs.concurrent.Akka
  *   import play.api.Play.current
  *
  *   val cb = CBExtension(Akka.system).buckets("bk1")
  * }}}
  *
  * 3. Use CouchbaseClient.
  * If use asynchronous methods then we can use CbFutureAsScala
  * to implicit convert spymemcache ListenableFuture to scala Future
  *
  * This code use play-json - a json parser library that do NOT depends on play framework
  * {{{
  *   import akka.contrib.couchbase.CbFutureAsScala._
  *   import play.api.libs.json.Json
  *
  *   case class User(name: String, age: Int)
  *
  *   implicit val fmt = Json.format[User]
  *
  *   def getOrCreate(key: String): Future[User] =
  *     cb.asyncGet(key).map{
  *       case s: String => Json.fromJson[User](Json.parse(s)).get
  *     }.recoverWith{
  *       //FIXME hard-code "Not found"
  *       case CBException("Not found") =>
  *         val u = User("Bob", 18)
  *         cb.set(key, Json.stringify(Json.toJson(u))).map(_ => u)
  *     }
  * }}}
  * */
class CBExtension(system: ExtendedActorSystem) extends Extension{
  /** Map from bucket name to CouchbaseClient
    * format of each bucket in config is similar to CouchbaseConnectionFactory */
  val buckets: Map[String, CouchbaseClient] = system.settings.config.
    getConfigList("akka.contrib.couchbase.buckets").asScala.map(bucketEntry).toMap

  private def bucketEntry(cfg: Config) = {
    val bucket = cfg.getString("bucket")
    val password = cfg.getString("password")
    val baseList = cfg.getString("nodes").split(';').toList.map(URI.create).asJava
    val cf = new CouchbaseConnectionFactory(baseList, bucket, password)
    val cb = new CouchbaseClient(cf)
    (bucket, cb)
  }

  system.registerOnTermination{
    buckets.foreach(_._2.shutdown())
  }
}

object CBExtension extends ExtensionId[CBExtension] with ExtensionIdProvider {
  def lookup = CBExtension

  def createExtension(system: ExtendedActorSystem) = new CBExtension(system)
}

