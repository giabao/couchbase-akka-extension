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
  import scala.language.implicitConversions

  class CBException(msg: String) extends Exception(msg)
  object CBException{
    def apply(msg: String) = new CBException(msg)
    def unapply(e: CBException) = if(e eq null) None else Some(e.getMessage)
  }

  implicit def operationFutureAsScala[T](underlying: OperationFuture[T]): Future[T] = {
    val p = Promise[T]()
    underlying.addListener(new OperationCompletionListener{
      def onComplete(f: OperationFuture[_]) {
        val status = f.getStatus //f is underlying
        if(status.isSuccess)
          p success underlying.get
        else
          p failure CBException(status.getMessage)
      }
    })
    p.future
  }

  implicit def getFutureAsScala[T](underlying: GetFuture[T]): Future[T] = {
    val p = Promise[T]()
    underlying.addListener(new GetCompletionListener{
      def onComplete(f: GetFuture[_]) {
        val status = f.getStatus //f is underlying
        if(status.isSuccess)
          p success underlying.get
        else
          p failure CBException(status.getMessage)
      }
    })
    p.future
  }

  /**
   * @note we don't need implicit converter from ViewFuture. Use HttpFuture[ViewResponse] instead
   */
  implicit def httpFutureAsScala[T](underlying: HttpFuture[T]): Future[T] = {
    val p = Promise[T]()
    underlying.addListener(new HttpCompletionListener{
      def onComplete(f: HttpFuture[_]) {
        val status = f.getStatus //f is underlying
        if(status.isSuccess)
          p success underlying.get
        else
          p failure CBException(status.getMessage)
      }
    })
    p.future
  }

  implicit def bulkGetFutureAsScala[T](underlying: BulkGetFuture[T]): Future[Map[String, T]] = {
    val p = Promise[Map[String, T]]()
    underlying.addListener(new BulkGetCompletionListener{
      def onComplete(f: BulkGetFuture[_]) {
        val status = f.getStatus //f is underlying
        if(status.isSuccess)
          p success underlying.get.asScala.toMap //java.util.Map -> mutable.Map -> immutable.Map
        else
          p failure CBException(status.getMessage)
      }
    })
    p.future
  }

//  implicit def replicaGetFutureAsScala[T](underlying: ReplicaGetFuture[T]): Future[T] = ??? //we don't need now
}
