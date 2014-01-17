package akka.contrib.couchbase

import net.spy.memcached.internal._
import scala.concurrent.{Future, Promise}
import com.couchbase.client.internal.{HttpCompletionListener, HttpFuture}
import collection.JavaConverters._

/** implicit convert [T extends net.spy.memcached.internal.ListenableFuture] to scala Future
  * @see http://stackoverflow.com/questions/11529145/how-do-i-wrap-a-java-util-concurrent-future-in-an-akka-future?rq=1
  *      http://stackoverflow.com/questions/17215421/scala-concurrent-future-wrapper-for-java-util-concurrent-future
  *      http://www.couchbase.com/issues/browse/JCBC-343#comment-68524 */
object CbFutureAsScala{
  class CBException(msg: String) extends Exception(msg)
  object CBException{
    def apply(msg: String) = new CBException(msg)
    def unapply(e: CBException) = if(e eq null) None else Some(e.getMessage)
  }
  val NotFound = "Not found"

  implicit class RichOperationFuture[T](underlying: OperationFuture[T]){
    def asScala: Future[T] = {
      val p = Promise[T]()
      lazy val listener: OperationCompletionListener = new OperationCompletionListener{
        def onComplete(f: OperationFuture[_]) {
          underlying.removeListener(listener)
          val status = f.getStatus //f is underlying
          if(status.isSuccess)
            p success underlying.get
          else
            p failure CBException(status.getMessage)
        }
      }
      underlying.addListener(listener)
      p.future
    }
  }

  implicit class RichGetFuture[T](underlying: GetFuture[T]){
    def asScala: Future[T] = {
      val p = Promise[T]()
      lazy val listener: GetCompletionListener = new GetCompletionListener{
        def onComplete(f: GetFuture[_]) {
          underlying.removeListener(listener)
          val status = f.getStatus //f is underlying
          if(status.isSuccess)
            p success underlying.get
          else
            p failure CBException(status.getMessage)
        }
      }
      underlying.addListener(listener)
      p.future
    }
  }

  /**
   * @note we don't need implicit converter from ViewFuture. Use HttpFuture[ViewResponse] instead
   */
  implicit class RichHttpFuture[T](underlying: HttpFuture[T]){
    def asScala: Future[T] = {
      val p = Promise[T]()
      lazy val listener: HttpCompletionListener = new HttpCompletionListener{
        def onComplete(f: HttpFuture[_]) {
          underlying.removeListener(listener)
          val status = f.getStatus //f is underlying
          if(status.isSuccess)
            p success underlying.get
          else
            p failure CBException(status.getMessage)
        }
      }
      underlying.addListener(listener)
      p.future
    }
  }

  implicit class RichBulkFuture[T](underlying: BulkFuture[java.util.Map[String, T]]){
    def asScala: Future[Map[String, T]] = {
      val p = Promise[Map[String, T]]()
      lazy val listener: BulkGetCompletionListener = new BulkGetCompletionListener{
        def onComplete(f: BulkGetFuture[_]) {
          underlying.removeListener(listener)
          val status = f.getStatus //f is underlying
          if(status.isSuccess)
            p success underlying.get.asScala.toMap //java.util.Map -> mutable.Map -> immutable.Map
          else
            p failure CBException(status.getMessage)
        }
      }
      underlying.addListener(listener)
      p.future
    }
  }

//  implicit class RichReplicaGetFuture[T](underlying: ReplicaGetFuture[T]){ ??? } //we don't need now
}
