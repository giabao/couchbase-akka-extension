/**
 * @author giabao
 * created: 2013-10-13 23:21
 * (c) 2011-2013 sandinh.com
 */
package akka.contrib.couchbase

import play.api.libs.json.{Format, Writes, Json, Reads}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.contrib.couchbase.CbFutureAsScala._
import collection.JavaConverters._
import com.couchbase.client.CouchbaseClient

trait WithCB{
  protected def cb: CouchbaseClient
}

trait CBReads[T] extends WithCB{
  protected def reads(v: AnyRef): T

  final def get(key: String): Future[T] = cb.asyncGet(key).asScala map reads

  final def getBulkImpl(keys: Seq[String]): Future[Seq[T]] =
    cb.asyncGetBulk(keys.asJava).asScala.map{bulk =>
      keys map bulk.apply map reads
    }

  final def getBulkOptionImpl(keys: Seq[String]): Future[Seq[Option[T]]] =
    cb.asyncGetBulk(keys.asJava).asScala.map{bulk =>
      keys map bulk.get map (o => o map reads)
    }
}

trait CBWrites[T] extends WithCB{
  protected def writes(v: T): String
  protected def Expiry = 0

  final def set(key: String, value: T): Future[java.lang.Boolean] = cb.set(key, Expiry, writes(value)).asScala

  final def setBulkImpl(keys: Seq[String], values: Seq[T]) = Future.traverse(keys zip values)(t => set(t._1, t._2))
}

trait CBFormat[T] extends CBReads[T] with CBWrites[T]

trait CBJsonReads[T] extends CBReads[T]{
  protected implicit val fmt: Reads[T]

  /** try parse s (must be a string) to T */
  protected final def reads(v: AnyRef): T = Json.fromJson[T](Json.parse(v.asInstanceOf[String])).get
}

trait CBJsonWrites[T] extends CBWrites[T]{
  protected implicit val fmt: Writes[T]

  protected final def writes(v: T) = Json.stringify(Json.toJson(v))
}

trait CBJson[T] extends CBJsonReads[T] with CBJsonWrites[T]{
  protected implicit val fmt: Format[T]
}

trait HasKey1[A]{
  protected def key(a: A): String
}

trait ReadsKey1[T, A] extends HasKey1[A]{this: CBReads[T] =>
  final def get(a: A): Future[T] = get(key(a))

  final def getBulk(l: Seq[A]): Future[Seq[T]] = getBulkImpl(l map key)

  final def getBulkOption(l: Seq[A]): Future[Seq[Option[T]]] = getBulkOptionImpl(l map key)
}

trait WritesKey1[T, A] extends HasKey1[A]{this: CBWrites[T] =>
  final def set(a: A, value: T): Future[java.lang.Boolean] = set(key(a), value)

  final def setBulk(l: Seq[A], values: Seq[T]) = setBulkImpl(l map key, values)
}

trait Key1[T, A] extends ReadsKey1[T, A] with WritesKey1[T, A]{this: CBReads[T] with CBWrites[T] =>
  final def delete(a: A) = cb.delete(key(a)).asScala
}

trait HasKey2[A, B]{
  protected def key(a: A, b: B): String
}

trait ReadsKey2[T, A, B] extends HasKey2[A, B]{this: CBReads[T] =>
  final def get(a: A, b: B): Future[T] = get(key(a, b))

  final def getBulk(l: Seq[A], b: B): Future[Seq[T]] = getBulkImpl(l.map(key(_, b)))

  final def getBulkOption(l: Seq[A], b: B): Future[Seq[Option[T]]] = getBulkOptionImpl(l.map(key(_, b)))
}

trait WritesKey2[T, A, B] extends HasKey2[A, B]{this: CBWrites[T] =>
  final def set(a: A, b: B, value: T): Future[java.lang.Boolean] = set(key(a, b), value)

  final def setBulk(l: Seq[A], b: B, values: Seq[T]) = setBulkImpl(l.map(key(_, b)), values)
}

trait Key2[T, A, B] extends ReadsKey2[T, A, B] with WritesKey2[T, A, B] {this: CBReads[T] with CBWrites[T] =>
  final def changeBulk(l: Seq[A], b: B)(f: Option[T] => T) = getBulkOption(l, b).
    flatMap{optionList =>
      val values = optionList map f
      setBulk(l, b, values)
    }

  final def delete(a: A, b: B) = cb.delete(key(a, b)).asScala
}
