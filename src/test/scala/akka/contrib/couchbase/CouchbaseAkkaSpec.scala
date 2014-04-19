/** @author giabao
  * created: 2013-10-07 17:45
  * (c) 2011-2013 sandinh.com */
package akka.contrib.couchbase

import org.specs2.mutable.Specification
import CbFutureAsScala._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._
import net.spy.memcached.ops.StatusCode.ERR_NOT_FOUND
import scala.concurrent.Await
import scala.concurrent.duration._

class CouchbaseAkkaSpec extends Specification with CBHelper {
  sequential

  val k = "test_key"

  "CouchbaseAkka" should {
    "connect to CB on start" in {
      CBExtension(system).buckets.size must be_>(0)
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

    "throw CBException(ERR_NOT_FOUND)" in {
      val k = "test_key_not_exist"
      val atMost = Duration(1, SECONDS)
      Await.result(cb.asyncGet(k).asScala, atMost) must throwA[CBException].like {
        case CBException(ERR_NOT_FOUND) => ok
      }

      val future = cb.asyncGet(k).asScala.recover {
        case CBException(ERR_NOT_FOUND) => throw new Exception("foo")
      }

      Await.result(future, atMost) must throwA[Exception]("foo")

      val v = "Bob"

      cb.set(k, v).get
      cb.asyncGet(k).asScala must beEqualTo(v).await

      cb.delete(k).asScala must beEqualTo(true).await
    }

    assertTerminate()
  }
}
