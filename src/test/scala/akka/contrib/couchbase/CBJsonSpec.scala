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

  trait WithMyCB extends WithCB{
    protected def cb = CBExtension(system).buckets.head._2
  }

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

  object Trophy extends CBJson[Trophy] with Key2[Trophy, Int, TrophyType] with WithMyCB{
    protected def key(uid: Int, t: TrophyType) = "u" + t.id + uid
    protected implicit val fmt = Json.format[Trophy]
  }

  //as of version 2.0.9, the following code will not compilable, with the following compile error:
  //overriding method set in trait WritesKey1 of type (a: String, value: Int)scala.concurrent.Future[Boolean];
  //[error]  method set in trait CBWrites of type (key: String, value: Int)scala.concurrent.Future[Boolean] cannot override final member
  //[error]   object WritesIntWithStringKey extends WritesKey1[Int, String] with CBWrites[Int] with WithMyCB{
  object WritesIntWithStringKey extends WritesKey1[Int, String] with CBWrites[Int] with WithMyCB{
    override protected def Expiry = 60
    protected def key(a: String) = s"test$a"
    protected def writes(v: Int) = v.toString
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

    /* The following code will throw StackOverflowError at line C.set("x")
trait A{
  def set(k: String){ println("set: " + k) }
}
trait B[X] extends A{
  def key(x: X): String
  def set(x: X): Unit = set(key(x))
}
object C extends B[String] with A{
  def key(x: String) = x
}
C.set("x")

      But if we define B.set as:
def set(x: X) = super.set(key(x))
      Then every thing is OK.

      And, if we declare
trait B[X] { this: A =>
      As we declare in version 2.0.9:
ReadsKey1[T, A] { this: CBReads[T] =>
      Then object C will not compilable. @see WritesIntWithStringKey
     */
    "not throws StackOverflowError" in {
      WritesIntWithStringKey.set("a", 1) must not (throwA[Exception])
    }

    "shutdown ActorSystem" in {
      system.shutdown()
      system.awaitTermination()

      system.isTerminated === true
    }
  }
}
