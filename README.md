couchbase-akka-extension
========================
This is an [akka extension](http://doc.akka.io/docs/akka/2.2.1/scala/extending-akka.html) for accessing [Couchbase 2](http://www.couchbase.com/).

### Sample usage
1. Add to application.conf

  ```
  akka{
    extensions = ["akka.contrib.couchbase.CBExtension"]
    contrib.couchbase.buckets = [
      { bucket=bk1
        # You can map bk1 to a different `real` bucket name in couchbase.
        # If cb-bucket setting is missing then CBExtension will use `bucket` (here is bk1).
        # One use-case of this setting is to support switching
        # deploy environment without changing any line of code
        cb-bucket="bk1_test"
        password="some password"
        nodes="http://cb1.sandinh.com:8091/pools;http://cb2.sandinh.com:8091/pools"
      },
      { bucket=bk2
        password="some password"
        nodes="http://cb3.sandinh.com:8091/pools"
      }
    ]
  }
  ```

2. Get CouchbaseClient from an ActorSystem.

  This example use Play framework 2 - in which, we can get ActorSystem by importing.
  Of course, you can get CouchbaseClient without Play.
  ```scala
  import play.api.libs.concurrent.Akka
  import play.api.Play.current
  
  val cb = CBExtension(Akka.system).buckets("bk1")
  ```

3. Use CouchbaseClient.
  If use asynchronous methods then we can use CbFutureAsScala
  to implicit convert spymemcache ListenableFuture to scala Future
  This code use play-json - a json parser library that do NOT depends on play framework
  ```scala
  import akka.contrib.couchbase.CbFutureAsScala._
  import play.api.libs.json.Json
  import net.spy.memcached.ops.StatusCode.ERR_NOT_FOUND
  import scala.concurrent.ExecutionContext.Implicits.global

  case class User(name: String, age: Int)
  
  implicit val fmt = Json.format[User]
  
  def getOrCreate(key: String): Future[User] =
    cb.asyncGet(key).asScala.map{
      case s: String => Json.fromJson[User](Json.parse(s)).get
    }.recoverWith{
      case CBException(ERR_NOT_FOUND) =>
        val u = User("Bob", 18)
        cb.set(key, Json.stringify(Json.toJson(u))).asScala.map(_ => u)
    }
  ```

### Install
This library is published to [maven central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.sandinh%22%20AND%20a%3A%22couchbase-akka-extension_2.10%22).

If you use sbt then:
```libraryDependencies += "com.sandinh" %% "couchbase-akka-extension" % "3.1.2"```

### Changelogs
We use [Semantic Versioning](http://semver.org), so changing in micro version is binary compatible.

Ex, v2.0.x is binary compatible with v2.0.0

##### v3.1.2
+ update sbt 0.13.5-RC4, scala 2.11.1, akka 2.3.3 & specs2 2.3.12
+ add convenient traits akka.contrib.couchbase.{HasKey0, ReadsKey0, WritesKey0, Key0}

##### v3.1.1
+ update couchbase-client 1.4.1. version 1.4.0 of couchbase-client depends on spymemcached 2.11.1 which has some critical bugs.

##### v3.1.0
+ Change nothing in code but update play-json (optional dependency) to 2.3.0-RC1 & akka-actor 2.3.2
+ cross compile for scala 2.11 & scala 2.10

##### v3.0.1
+ update couchbase-client 1.4.0 & spymemcached 2.11.1
+ update sbt 0.13.2

##### v3.0.0
+ Only change structure of CBException to use StatusCode type-safe feature in spymemcached 2.10.6.
 @see [SPY-153](http://www.couchbase.com/issues/browse/SPY-153)
  & the [corresponding commit](https://github.com/couchbase/spymemcached/commit/eb4c019f919370c9993d4a58d4990574b58d0f1e)

###### Migration guide from v2.x:
In v2.x, you check whether the get operation return NotFound by matching
```case CBException(NotFound)``` where NotFound is a string, hardcode = "Not found".

Now, in v3.x, this checking must change to
```case CBException(ERR_NOT_FOUND)``` where ERR_NOT_FOUND is a value in enum net.spy.memcached.ops.StatusCode.

Of course, you can check with other StatusCode.

##### v2.1.3
+ support `cb-bucket` setting. This version is backward compatible with 2.1.x

##### v2.1.2
+ update play 2.2.2 & akka 2.2.4

##### v2.1.0
+ Add scala doc for CBJson.scala
+ Remove handler when future completed in [CbFutureAsScala](https://github.com/giabao/couchbase-akka-extension/blob/master/src/main/scala/akka/contrib/couchbase/CbFutureAsScala.scala)
+ Change ```ReadsKey1[T, A] { this: CBReads[T] =>``` to ```ReadsKey1[T, A] extends CBReads[T] {```. This change can break binary compatibility, but keep source compatibility. @See case "not throws StackOverflowError" in [CBJsonSpec](https://github.com/giabao/couchbase-akka-extension/blob/master/src/test/scala/akka/contrib/couchbase/CBJsonSpec.scala) for more information.

##### v2.0.8
+ fix [IllegalStateException: Promise already completed. in RichBulkFuture](https://github.com/giabao/couchbase-akka-extension/issues/2)

##### v2.0.7
+ fix [UnsupportedOperationException](https://github.com/giabao/couchbase-akka-extension/issues/1)

##### v2.0.4
+ only update akka-actor 2.2.3

##### v2.0.3
+ update akka-actor 2.2.2
+ add [CBJson util traits](https://github.com/giabao/couchbase-akka-extension/blob/master/src/main/scala/akka/contrib/couchbase/CBJson.scala) for reads/ writes couchbase.
  See sample usage in [CBJsonSpec](https://github.com/giabao/couchbase-akka-extension/blob/master/src/test/scala/akka/contrib/couchbase/CBJsonSpec.scala)
+ add optional dependency `"com.typesafe.play"         %% "play-json"          % "2.2.0"   % "optional"`.
  This is mandatory if you use CBJson.

##### v2.0.2
+ update couchbase-client 1.2.1 (& spymemcached 2.10.1)

##### v2.0.1
+ add val akka.contrib.couchbase.CbFutureAsScala.NotFound

##### v2.0.0
+ Add unit test
+ (NOT compatible) change from:
```scala
implicit def xxCbFutureAsScala[T](underlying: XxCbFuture[T]): Future[T]
```
to:
```scala
implicit class RichXxCbFuture(underlying: XxCbFuture[T]){
  def asScala: Future[T]
}
```
So, in v2.0.0, you must call .asScala to convert CbFuture to scala Future (similar to collection.JavaConverters._ )

##### v1.0.0
First stable release

### Licence
This software is licensed under the Apache 2 license:
http://www.apache.org/licenses/LICENSE-2.0

Copyright 2013 Sân Đình (http://sandinh.com)
