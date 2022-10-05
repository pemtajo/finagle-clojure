(ns finagle-clojure.builder.server
  "Functions for creating and altering `com.twitter.finagle.Server` objects independent
  of any particular codec. Generally speaking codec-specific server functions
  should be preferred, but these are included for comptability with older systems
  configured at the server level."
  (:require [finagle-clojure.scala :as scala]
            [erp12.fijit.alpha.reflect :as r])
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
           (com.twitter.finagle.dispatch SerialServerDispatcher)
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
          (Manifest/Any)
          (Manifest/Any)))

(defn dispatcher-builder
  [service]
  (scala/Function [t] (SerialServerDispatcher. t service)))

(defn address
  [p]
  (InetSocketAddress. (int p)))

(defn ^Future close!
  "Stops the given Server.

  *Arguments*:

    * `server`: an instance of [[com.twitter.finagle.builder.Server]]

  *Returns*:

    a Future that closes when the server stops"
  [^ListeningServer server]
  (.close server))
