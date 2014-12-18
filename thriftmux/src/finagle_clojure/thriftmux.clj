(ns finagle-clojure.thriftmux
  "Functions for creating ThriftMux clients & servers from Java classes generated
  from a Thrift service definition using [Scrooge](https://twitter.github.io/scrooge/).
  
  The lein-finagle-clojure plugin can be used to compile Thrift definitions to Java with Scrooge.
  
  See:
  * test/clj/finagle_clojure/thriftmux_test.clj"
  (:require [finagle-clojure.thrift :as thrift])
  (:import [com.twitter.finagle ListeningServer Service ThriftMux]))

;; TODO should thriftmux serve a thrift service? or is it ok that service is pretty much duplicated from thrift
;; since it makes the interface a little easier to use.

(defmacro service
  "Sugar for implementing a com.twitter.finagle.Service based on the
  interface defined in `qualified-service-class-name`. The appropriate
  Finagle interface for that class will automatically be imported.
  Provide an implementation for it like `proxy` (`this` is an implicit argument).

  The Finagle interface for a Service class generated by Scrooge will wrap the response
  type of a method in Future so it is asynchronous.

  *Arguments*:

    * `qualified-service-class-name`: This class's Finagled interface will automatically be imported.
        e.g. if you pass MyService then MyService$ServiceIface will be imported and used.
    * `body`: the implementation of this service. Methods should be defined without an explicit `this` argument.

  *Returns*:

  A new `Service`."
  [service-class-name & body]
  `(do
     (import ~(thrift/finagle-interface service-class-name))
     (proxy [~(thrift/finagle-interface service-class-name)] []
       ~@body)))

(defn serve
  "Serve `service` on `addr`. Use this to actually run your ThriftMux service.
  Note that this will not block while serving.
  If you want to wait on this use [[finagle-clojure.futures/await]].

  *Arguments*:

    * `addr`: The port on which to serve.
    * `service`: The Service that should be served.

  *Returns*:

  A new com.twitter.finagle.ListeningServer."
  [^String addr ^Service service]
  (ThriftMux/serveIface addr service))

(defn announce*
  "Announce this server to the configured load balancer.

  *Arguments*:
  * `path`: a String represent the path on the load balancer
  * `server`: a ListeningServer (returned by [serve])
  
  *Returns*:
  
  A Future[Announcement].

  *See*:
  [[announce]], [https://twitter.github.io/finagle/guide/Names.html]"
  [ path ^ListeningServer server]
  (.announce server path))

(defn announce
  "Announce this server to the configured load balancer.

  This functions the same as [[announce*]] but returns the `server` passed in
  so it can be chained together like:

  ````clojure
  (->> service
       (thriftmux/serve \":9999\")
       (thriftmux/announce \"zk!localhost!/path/to/nodes\")
       (f/await))
  ````

  *Arguments*:
  * `path`: a String representing the path on the load balancer
  * `server`: a ListeningServer (returned by [serve])
  
  *Returns*:
  
  `server`

  *See*:
  [[announce*]], [https://twitter.github.io/finagle/guide/Names.html]"
  [path ^ListeningServer server]
  (announce* path server)
  server)

(defmacro client
  "Sugar for creating a client for a compiled ThriftMux service.
  The appropriate Finagle interface for that class will automatically be imported.
  Note that operations on this client will return a Future representing the result of an call
  This is meant to show that this client can make an RPC call and may be expensive to invoke.

  E.g. if a ThriftMux service definition has a method called `doStuff` you can call it on a client
  like this `(.doStuff client)`. 

  *Arguments*:

    * `addr`: Where to find the ThriftMux server.
    * `qualified-service-class-name`: This class's Finagled interface will automatically be imported.
        e.g. if you pass MyService then MyService$ServiceIface will be imported and used.

  *Returns*:

  A new client."
  [addr client-iterface-class]
  `(do
     (import ~(thrift/finagle-interface client-iterface-class))
     (ThriftMux/newIface ~addr ~(thrift/finagle-interface client-iterface-class))))