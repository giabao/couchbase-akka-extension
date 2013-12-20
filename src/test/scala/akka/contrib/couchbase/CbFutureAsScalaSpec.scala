package akka.contrib.couchbase

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import CbFutureAsScala._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._
import scala.concurrent.Future

class CbFutureAsScalaSpec extends Specification{sequential
  lazy val system = ActorSystem()
  lazy val cb = CBExtension(system).buckets.head._2
  lazy val keys = Range(0, 1000).map("exist" + _)

  "RichBulkFuture" should {
    "asyncGetBulk all exist documents successfully" in {
      val futureOfBoolSeq = Future.traverse(keys)(k => cb.set(k, 60, "value of " + k).asScala)
      val futureOfAllTrue = futureOfBoolSeq.map(_.forall(_.booleanValue))
      futureOfAllTrue must beTrue.await

      cb.asyncGetBulk(keys.asJavaCollection).asScala.map(_.size) must be_==(keys.size).await
    }

    "asyncGetBulk all non-exist documents successfully" in {
      cb.asyncGetBulk("not_exist1", "not_exist2").asScala.map{m =>
        m.size
      } must be_==(0).await
    }

    "asyncGetBulk all some-exist documents successfully" in {
      val allKeys = Range(0, 2000).map("not_exist" + _) ++ keys
      cb.asyncGetBulk(allKeys.asJavaCollection).asScala.map(_.size) must be_==(keys.size).await
    }

    "shutdown ActorSystem" in {
      system.shutdown()
      system.awaitTermination()

      system.isTerminated === true
    }
  }
}
