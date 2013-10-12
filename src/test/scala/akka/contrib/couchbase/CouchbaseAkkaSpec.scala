package akka.contrib.couchbase

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import com.couchbase.client.CouchbaseClient
import CbFutureAsScala._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._

/**
 * @author giabao
 * created: 2013-10-07 17:45
 * Copyright(c) 2011-2013 sandinh.com
 */
class CouchbaseAkkaSpec extends Specification{sequential
  var system: ActorSystem = _
  var cb: CouchbaseClient = _

  val k = "test_key"

  "CouchbaseAkka" should {
    "connect to CB on start" in {
      system = ActorSystem()
      CBExtension(system).buckets.size must be_>(0)
      cb = CBExtension(system).buckets.head._2
      val v = "test_value"
      cb.set(k, 10, v).get
      cb.get(k) === v
    }

    "convert RichOperationFuture" in {
      val v = 1
      val f = cb.set(k, 10, v).asScala.map(_.booleanValue)
      f must beTrue.await
    }

    "convert RichGetFuture" in {
      cb.asyncGet(k).asScala.mapTo[Int].map(_ + 1) must beEqualTo(2).await
    }

    "convert RichBulkFuture" in {
      val k2 = "test_key2"
      val v2 = Some("v2")
      val keys = Seq(k, k2)
      cb.set(k2, v2).get
      cb.asyncGetBulk(keys.asJava).asScala must havePair(k2 -> v2.asInstanceOf[AnyRef]).await
    }

    """throw CBException(NotFound)""" in {
      val k = "test_key_not_exist"
      cb.asyncGet(k).asScala must throwA(CBException(NotFound)).await
      cb.asyncGet(k).asScala.recover{
        case CBException(NotFound) => throw new CBException("foo")
      }.recover{
        case CBException("bar") => "success"
      } must throwA(CBException("foo")).await

      val v = "Bob"

      cb.set(k, v).get
      cb.asyncGet(k).asScala must beEqualTo(v).await

      cb.delete(k).asScala must beEqualTo(true).await
    }

    "disconnect when ActorSystem is terminated" in {
      system.shutdown()
      system.awaitTermination()

      system.isTerminated === true
      cb.get(k) should throwAn[IllegalStateException]
    }
  }
}
