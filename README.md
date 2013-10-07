couchbase-akka-extension
========================
This is an [akka extension](http://doc.akka.io/docs/akka/2.2.1/scala/extending-akka.html) for accessing [Couchbase 2](http://www.couchbase.com/).

### Sample usage
1. Add to application.conf
```
  akka.contrib.couchbase.buckets = [
    { bucket=bk1
      password="some password"
      nodes="http://cb1.sandinh.com:8091/pools;http://cb2.sandinh.com:8091/pools"
    },
    { bucket=bk2
      password="some password"
      nodes="http://cb3.sandinh.com:8091/pools"
    }
  ]
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
  
  case class User(name: String, age: Int)
  
  implicit val fmt = Json.format[User]
  
  def getOrCreate(key: String): Future[User] =
    cb.asyncGet(key).map{
      case s: String => Json.fromJson[User](Json.parse(s)).get
    }.recoverWith{
      //FIXME hard-code "Not found"
      case CBException("Not found") =>
        val u = User("Bob", 18)
        cb.set(key, Json.stringify(Json.toJson(u))).map(_ => u)
    }
```
