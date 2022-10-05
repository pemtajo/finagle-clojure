(ns finagle-clojure.builder.server
  "Functions for creating and altering `com.twitter.finagle.Server` objects independent
  of any particular codec. Generally speaking codec-specific server functions
  should be preferred, but these are included for comptability with older systems
  configured at the server level."
  (:require [finagle-clojure.scala :as scala])
  (:import (com.twitter.finagle.server StdStackServer StackServer StackServer$)
           (com.twitter.finagle Service ListeningServer)
           (com.twitter.finagle.netty4 Netty4Listener Netty4Listener$)
           (java.net InetSocketAddress)
           (io.netty.channel Channel ChannelPipeline ChannelFactory ChannelInitializer)
           (io.netty.handler.codec.string StringEncoder StringDecoder)
           (io.netty.handler.codec DelimiterBasedFrameDecoder Delimiters)
           (io.netty.util CharsetUtil)
           (scala.reflect Manifest Manifest$)
           (com.twitter.util Duration Future)
           (com.twitter.finagle.tracing Tracer)
           (com.twitter.finagle.stats StatsReceiver)
           (com.twitter.finagle.server StackBasedServer)))

(defn string-pipeline-builder
  [^ChannelPipeline p]
  (doto p
    (.addLast "line" (new DelimiterBasedFrameDecoder 100 (Delimiters/lineDelimiter)))
    (.addLast "stringDecoder" (new StringDecoder (CharsetUtil/UTF_8)))
    (.addLast "stringEncoder" (new StringEncoder (CharsetUtil/UTF_8)))))

(defn pipeline-initializer
  [pipeline-builder]
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (pipeline-builder ^ChannelPipeline (.pipeline ch)))))

(defn ^Netty4Listener netty4listener
  []
  (.apply Netty4Listener$/MODULE$
          (scala/Function [p] (string-pipeline-builder p))
          (.defaultParams StackServer$/MODULE$)
          3                             ;; Expects class scala.reflect.Manifest
          4)
  #_(Netty4Listener/apply (scala/Function [p] (string-pipeline-builder p))
                        (.defaultParams StackServer$/MODULE$)
                        3
                        4)
  #_(Netty4Listener$/$anonfun$apply$2 (pipeline-initializer string-pipeline-builder)))

#_(defn ^ServerBuilder builder
    "A handy Builder for constructing Servers (i.e., binding Services to a port).
  The `ServerBuilder` requires the definition of `codec`, `bind-to` and `named`.

  The main class to use is [[com.twitter.finagle.builder.ServerBuilder]], as so:

  ```
    (-> (builder)
        (named \"servicename\")
        (bind-to 3000)
        (build some-service))
  ```

  *Arguments*:

    * None.

  *Returns*:

    a new instance of [[com.twitter.finagle.builder.ServerBuilder]]."
    []
    (ServerBuilder/apply))



#_(defn ^ListeningServer build
  "Given a completed `ServerBuilder` and a `Service`, constructs an `Server` which is capable of
  responding to requests.

  *Arguments*:

    * `b`: a ServerBuilder
    * `svc`: the Service this server will use to respond to requests

  *Returns*:

    a running instance of [[com.twitter.finagle.builder.Server]]"
  [^ServerBuilder b ^Service svc]
  (.unsafeBuild b svc))

(defn ^Future close!
  "Stops the given Server.

  *Arguments*:

    * `server`: an instance of [[com.twitter.finagle.builder.Server]]

  *Returns*:

    a Future that closes when the server stops"
  [^ListeningServer server]
  (.close server))

#_(defn ^ServerBuilder named
  "Configures the given ServerBuilder with a name.

  *Arguments*:

    * `b`: a ServerBuilder
    * `name`: the name of this server

  *Returns*:

    a named ServerBuilder"
  [^ServerBuilder b ^String name]
  (.name b name))

#_(defn ^ServerBuilder bind-to
  "Configures the given ServerBuilder with a port.

  *Arguments*:

    * `b`: a ServerBuilder
    * `p`: the port number to bind this server to

  *Returns*:

    a bound ServerBuilder"
  [^ServerBuilder b p]
  (.bindTo b (InetSocketAddress. (int p))))

#_(defn ^ServerBuilder request-timeout
  "Configures the given ServerBuilder with a request timeout.

  *Arguments*:

    * `b`: a ServerBuilder
    * `d`: the duration of the request timeout for this server

  *Returns*:

    a ServerBuilder configured with the given timeout"
  [^ServerBuilder b ^Duration d]
  (.requestTimeout b d))

#_(defn ^ServerBuilder stack
  "Configures the given ServerBuilder with a codec.

  *Arguments*:

    * `b`: a ServerBuilder
    * `sbs`: a StackBasedServer

  *Returns*:

    a ServerBuilder configured with the given codec"
  [^ServerBuilder b ^StackBasedServer sbs]
  (.stack b sbs))

#_(defn ^ServerBuilder max-concurrent-requests
  "Configures the given ServerBuilder to accept a maximum number of concurrent requests.

  *Arguments*:

    * `b`: a ServerBuilder
 Netty4Listener.scala   * `mcr`: the maximum number of concurrent requests

  *Returns*:

    a ServerBuilder configured with a maximum number of concurrent requests"
  [^ServerBuilder b mcr]
  (.maxConcurrentRequests b (int mcr)))

#_(defn ^ServerBuilder tracer
  "Configures the given ServerBuilder to use a Tracer.

  *Arguments*:

    * `b`: a ServerBuilder
    * `tracer`: a Tracer

  *Returns*:

    a ServerBuilder configured with the given tracer"
  [^ServerBuilder b ^Tracer tracer]
  (.tracer b tracer))

#_(defn ^ServerBuilder report-to
  "Configures the given ServerBuilder to report to a stats receiver.

  *Arguments*:

    * `b`: a ServerBuilder
    * `rcvr`: a StatsReceiver

  *Returns*:

    a ServerBuilder configured with the given stats receiver"
  [^ServerBuilder b ^StatsReceiver rcvr]
  (.reportTo b rcvr))
