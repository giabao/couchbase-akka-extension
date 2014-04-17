/** @author giabao
  * created: 2013-10-13 23:21
  * (c) 2011-2013 sandinh.com */
package akka.contrib.couchbase

import play.api.libs.json.{Format, Writes, Json, Reads}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.contrib.couchbase.CbFutureAsScala._
import collection.JavaConverters._
import com.couchbase.client.CouchbaseClient

/** Base trait to interact with CouchbaseClient */
trait WithCB {
  protected def cb: CouchbaseClient
}

/** This trait implement methods get, getBulkImpl, getBulkOptionImpl based on a need-impl method reads: Object => T
  * @tparam T type of the data class that we want to read from CB */
trait CBReads[T] extends WithCB {
  /** read an Object (gotten from CB) to the data object of type T
    * @param v the Object gotten from CB
    * @return the data object of type T */
  protected def reads(v: AnyRef): T

  /** Get the document
    * @param key CB key
    * @return a Future of T (when get success, the gotten value can be null depends on the implement of CBReads[T].reads)
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException CBException(ERR_NOT_FOUND) when key not found, or other CBException
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def get(key: String): Future[T] = cb.asyncGet(key).asScala map reads

  /** Bulk get documents
    * @param keys a Seq of CB keys
    * @return a Future of Seq of T (can be null depends on the implement of CBReads[T].reads)
    * @throws java.util.NoSuchElementException insteads of CBException(ERR_NOT_FOUND) when some keys not found
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def getBulkImpl(keys: Seq[String]): Future[Seq[T]] =
    cb.asyncGetBulk(keys.asJava).asScala.map { bulk =>
      keys map bulk.apply map reads
    }

  /** Bulk get documents
    * @param keys a Seq of CB keys
    * @return a Future of Seq of Option[T]
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def getBulkOptionImpl(keys: Seq[String]): Future[Seq[Option[T]]] =
    cb.asyncGetBulk(keys.asJava).asScala.map { bulk =>
      keys map bulk.get map (o => o map reads)
    }
}

/** This trait implement methods set, setBulkImpl based on a need-impl method writes: T => String
  * @tparam T type of the data class that we want to write to CB */
trait CBWrites[T] extends WithCB {
  protected def writes(v: T): String
  protected def Expiry = 0

  /** Set the document to CB
    * @return a Future of java.lang.Boolean
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail */
  final def set(key: String, value: T) = cb.set(key, Expiry, writes(value)).asScala

  /** Bulk set documents
    * @param keys a Seq of CB keys
    * @param values a Seq of data to store to CB. `keys` and `values` is mapped 1-1
    * @return a Future
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail */
  final def setBulkImpl(keys: Seq[String], values: Seq[T]) = Future.traverse(keys zip values)(t => set(t._1, t._2))
}

/** A convenient trait that mix CBReads & CBWrites
  * @tparam T type of the data class that we want to read from/ write to CB */
trait CBFormat[T] extends CBReads[T] with CBWrites[T]

/** This trait implement method CBReads.reads by using play.api.libs.json.Reads
  * @tparam T type of the data class that we want to read from CB */
trait CBJsonReads[T] extends CBReads[T] {
  protected implicit val fmt: Reads[T]

  /** try parse s (must be a string) to T */
  protected final def reads(v: AnyRef): T = Json.fromJson[T](Json.parse(v.asInstanceOf[String])).get
}

/** This trait implement method CBWrites.writes by using play.api.libs.json.Writes
  * @tparam T type of the data class that we want to write to CB */
trait CBJsonWrites[T] extends CBWrites[T] {
  protected implicit val fmt: Writes[T]

  protected final def writes(v: T) = Json.stringify(Json.toJson(v))
}

/** This trait implement method CBReads.reads and CBWrites.writes by using play.api.libs.json.Format
  * @tparam T type of the data class that we want to read from CB */
trait CBJson[T] extends CBJsonReads[T] with CBJsonWrites[T] {
  protected implicit val fmt: Format[T]
}

/** This trait has a method to map a param of type A to a CB key */
trait HasKey1[A] {
  /** Map a param of type A to a CB key
    * @return CB key */
  protected def key(a: A): String
}

/** This trait extends HasKey1 and contains some method to read CB documents
  * @tparam T type of the data class that we want to read from CB
  * @tparam A type of the param being used to generate a key of a CB document */
trait ReadsKey1[T, A] extends HasKey1[A] with CBReads[T] {
  /** Get the document has key generated from param a
    * @param a be used to generate CB key
    * @return a Future of T (when get success, the gotten value can be null depends on the implement of CBReads[T].reads)
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException CBException(ERR_NOT_FOUND) when key not found, or other CBException
    * @see [[akka.contrib.couchbase.HasKey1#key]] */
  final def get(a: A): Future[T] = super.get(key(a))

  /** Bulk get documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @return a Future of Seq of T (can be null depends on the implement of CBReads[T].reads)
    * @throws java.util.NoSuchElementException insteads of CBException(ERR_NOT_FOUND) when some keys not found
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey1#key]]
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def getBulk(l: Seq[A]): Future[Seq[T]] = getBulkImpl(l map key)

  /** Bulk get documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @return a Future of Seq of Option[T]
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey1#key]]
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def getBulkOption(l: Seq[A]): Future[Seq[Option[T]]] = getBulkOptionImpl(l map key)
}

/** This trait extends HasKey1 and contains some method to write/ delete CB documents
  * @tparam T type of the data class that we want to write to CB
  * @tparam A type of the param being used to generate a key of a CB document */
trait WritesKey1[T, A] extends HasKey1[A] with CBWrites[T] {
  /** Delete the document has key generated from param a
    * @param a be used to generate CB key
    * @return a Future of java.lang.Boolean
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey1#key]] */
  final def delete(a: A) = cb.delete(key(a)).asScala

  /** Set the document has key generated from param a
    * @param a be used to generate CB key
    * @return a Future of java.lang.Boolean
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey1#key]] */
  final def set(a: A, value: T) = super.set(key(a), value)

  /** Bulk set documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @param values a Seq of data to store to CB. `l` and `values` is mapped 1-1
    * @return a Future
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey1#key]] */
  final def setBulk(l: Seq[A], values: Seq[T]) = setBulkImpl(l map key, values)
}

/** This trait mix ReadsKey1 with WritesKey1
  * @tparam T type of the data class that we want to read from and write to CB
  * @tparam A type of the param being used to generate the a of a CB document */
trait Key1[T, A] extends ReadsKey1[T, A] with WritesKey1[T, A]

/** This trait has a method to map 2 param of type A, B to a CB key */
trait HasKey2[A, B] {
  /** Map 2 param of type A, B to a CB key
    * @return CB key */
  protected def key(a: A, b: B): String
}

/** This trait extends HasKey2 and contains some method to read CB documents
  * @tparam T type of the data class that we want to read from CB
  * @tparam A type of the first param being used to generate a key of a CB document
  * @tparam B type of the second param being used to generate a key of a CB document */
trait ReadsKey2[T, A, B] extends HasKey2[A, B] with CBReads[T] {
  /** Get the document has key generated from 2 params a, b
    * @param a be used to generate CB key
    * @param b be used to generate CB key
    * @return a Future of T (when get success, the gotten value can be null depends on the implement of CBReads[T].reads)
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException CBException(ERR_NOT_FOUND) when key not found, or other CBException
    * @see [[akka.contrib.couchbase.HasKey2#key]] */
  final def get(a: A, b: B): Future[T] = super.get(key(a, b))

  /** Bulk get documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @param b a value of type B, will be used with seq `l` to generate a Seq of CB keys
    * @return a Future of Seq of T (can be null depends on the implement of CBReads[T].reads)
    * @throws java.util.NoSuchElementException insteads of CBException(ERR_NOT_FOUND) when some keys not found
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey2#key]]
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def getBulk(l: Seq[A], b: B): Future[Seq[T]] = getBulkImpl(l.map(key(_, b)))

  /** Bulk get documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @param b a value of type B, will be used with seq `l` to generate a Seq of CB keys
    * @return a Future of Seq of Option[T]
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey2#key]]
    * @see [[akka.contrib.couchbase.CBReads#reads]] */
  final def getBulkOption(l: Seq[A], b: B): Future[Seq[Option[T]]] = getBulkOptionImpl(l.map(key(_, b)))
}

/** This trait extends HasKey2 and contains some method to write/ delete CB documents
  * @tparam T type of the data class that we want to write to CB
  * @tparam A type of the first param being used to generate a key of a CB document
  * @tparam B type of the second param being used to generate a key of a CB document */
trait WritesKey2[T, A, B] extends HasKey2[A, B] with CBWrites[T] {
  /** Delete the document has key generated from 2 params a, b
    * @param a be used to generate CB key
    * @param b be used to generate CB key
    * @return a Future of java.lang.Boolean
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey2#key]] */
  final def delete(a: A, b: B) = cb.delete(key(a, b)).asScala

  /** Set the document has key generated from 2 params a, b
    * @param a be used to generate CB key
    * @param b be used to generate CB key
    * @return a Future of java.lang.Boolean
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey2#key]] */
  final def set(a: A, b: B, value: T) = super.set(key(a, b), value)

  /** Bulk set documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @param b a value of type B, will be used with seq `l` to generate a Seq of CB keys
    * @param values a Seq of data to store to CB. `l` and `values` is mapped 1-1
    * @return a Future
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey2#key]] */
  final def setBulk(l: Seq[A], b: B, values: Seq[T]) = setBulkImpl(l.map(key(_, b)), values)
}

/** This trait mix ReadsKey2 with WritesKey2
  * @tparam T type of the data class that we want to read from and write to CB
  * @tparam A type of the first param being used to generate the a of a CB document
  * @tparam B type of the second param being used to generate the a of a CB document */
trait Key2[T, A, B] extends ReadsKey2[T, A, B] with WritesKey2[T, A, B] {
  /** Bulk get & change documents
    * @param l a Seq of type A, will be used to generate a Seq of CB keys
    * @param b a value of type B, will be used with seq `l` to generate a Seq of CB keys
    * @param f for each generated key, we will get the document from CB (then we will have a Option[T]).
    * This function `f` is used to transform the gotten Option[T] to a value (of type T) to set to CB
    * @return a Future
    * @throws akka.contrib.couchbase.CbFutureAsScala.CBException when the underlying CouchbaseClient's method fail
    * @see [[akka.contrib.couchbase.HasKey2#key]] */
  final def changeBulk(l: Seq[A], b: B)(f: Option[T] => T) = getBulkOption(l, b).
    flatMap { optionList =>
      val values = optionList map f
      setBulk(l, b, values)
    }
}
