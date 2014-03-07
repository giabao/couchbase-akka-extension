package akka.contrib.couchbase

import akka.actor.ActorSystem
import org.specs2.mutable.Specification

trait CBHelper { this: Specification =>
  lazy val system = ActorSystem()
  lazy val cb = CBExtension(system).buckets("bk1")

  def assertTerminate() = "disconnect when ActorSystem is terminated" in {
      system.shutdown()
      system.awaitTermination()

      system.isTerminated === true
      cb.get("test_key") should throwAn[IllegalStateException]
    }
}
