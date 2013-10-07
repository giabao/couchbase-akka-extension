package akka.contrib.couchbase

import akka.actor.{ExtendedActorSystem, ExtensionIdProvider, ExtensionId, Extension}
import com.couchbase.client.{CouchbaseConnectionFactory, CouchbaseClient}
import com.typesafe.config.Config
import java.net.URI
import collection.JavaConverters._

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

