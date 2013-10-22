/**
 * @author giabao
 * created: 2013-10-22 10:23
 * (c) 2011-2013 sandinh.com
 */
package akka.contrib.couchbase

import play.api.libs.json.Json
import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import CbFutureAsScala._

object CBJsonSpec {
  lazy val system = ActorSystem()

  sealed trait TrophyType{
    def id: Int
    def name: String
    def bonus: Long

    import TrophyType._
    final def luuDanh = this != TuTai
  }

  object TrophyType{
    case object TuTai extends TrophyType{
      val id = 0
      val name = "Tú Tài"
      val bonus = 10000000L
    }
    case object CuNhan extends TrophyType{
      val id = 1
      val name = "Cử Nhân"
      val bonus = 150000000L
    }
  }

  case class Trophy(awardCount: Int)

  object Trophy extends CBJson[Trophy] with Key2[Trophy, Int, TrophyType] {
    protected def cb = CBExtension(system).buckets.head._2
    protected def key(uid: Int, t: TrophyType) = "u" + t.id + uid
    protected implicit val fmt = Json.format[Trophy]
  }
}

class CBJsonSpec extends Specification{sequential
  import CBJsonSpec._, TrophyType._

  "CBJson" should {
    "accessible with Key2" in {
      Trophy.set(1, TuTai, Trophy(2)).map(_.booleanValue) must beTrue.await

      val uids = Seq(1,2,3)
      val trophies = Seq(Trophy(5), Trophy(6), Trophy(7))
      Await.result(Trophy.setBulk(uids, CuNhan, trophies), Duration(2, SECONDS)) must have size uids.size

      Trophy.get(1, CuNhan).map(_.awardCount) must beEqualTo(5).await

      Trophy.delete(1, CuNhan).map(_.booleanValue) must beTrue.await

      Trophy.get(1, CuNhan) must throwA(CBException(NotFound)).await
    }

    "shurtdown ActorSystem" in {
      system.shutdown()
      system.awaitTermination()

      system.isTerminated === true
    }
  }
}
