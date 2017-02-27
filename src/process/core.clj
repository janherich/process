(ns process.core
  (:require [clojure.core.async :as async :refer [chan go <! >! put! close! take!]]
            [process.instance :as instance]
            [process.execution :as execution]))

(comment
  "The `:task` type node represents points of external input into
   the process instance, when channel is returned, it will block."

  "The `:action` type node always contains an function called
   perform, which will be called with the process context and should
   return updated process context."

  "The gateway type node proceed function is called with the process
   context as an sole argument, it should return set of edges, where
   executions are propagated."

  "The process context map contains various data, which can be different
   for different nodes - `:gateway` nodes will have `:incoming`/`:outgoing`
   edges and `:pending` execution pointers in the context, all nodes
   except `:start` and `:end` will have the `:node-input` channel associated
   into context.")

(def environment (atom {}))

(def process-v2
  {:name "Run integration"
   :created #inst "2017-02-16T21:42:57"
   :version 1
   :doc "First version of the process"
   :process-graph {:nodes #{{:id 0
                             :type :event/start}
                            {:id 1
                             :name "List project files"
                             :type :task/action
                             :action-fn (fn [ctx]
                                          ;; faking the files listing call
                                          (assoc ctx :project-files-size (rand-int 4096)))}
                            {:id 2
                             :name "Write manifest ?"
                             :type :task/input
                             :context-key :write-manifest?}
                            {:id 3
                             :name "Write manifest ?"
                             :type :gateway/decision
                             :decisions [[#{4} :write-manifest?]]}
                            {:id 4
                             :name "Write manifest"
                             :type :task/action
                             :action-fn (fn [{:keys [project-files-size] :as ctx}]
                                          ;; faking the manifest write
                                          (swap! environment
                                                 assoc :manifest
                                                 (format "Manifest contains %s files"
                                                         project-files-size))
                                          (assoc ctx :manifest-written true))}
                            {:id 5
                             :type :event/end}}
                   :edges #{{:from 0 :to 1}
                            {:from 1 :to 2}
                            {:from 2 :to 3}
                            {:from 3 :to 4}
                            {:from 3 :to 5}
                            {:from 4 :to 5}}}})

(comment
  (def process-instance
    {:id #uuid "7da64230-137f-45c5-aba5-e114f674c5c8"
     :process process-v1
     :nodes {} ;; normalized nodes
     :id->nodes {} ;; normalized nodes indexed by id
     :runtime-state (atom
                     {;; when process execution goes parallel, there may be more execution edges
                      :execution-edges #{{:from 0 :to 1}}
                      ;; empty context
                      :ctx {}})
     ;; process input channel
     :process-input (chan)
     ;; node input channels
     :node-inputs {1 (chan)
                   2 (chan)
                   3 (chan)
                   4 (chan)}}))

(comment
  (put! (:process-input process-instance) {:node-id 3 :data true :actor "Smith"}))

(comment
  "Process entity relations"
  Process         <-one-to-one->  ProcessGraph
  ProcessGraph    <-one-to-many-> GraphNode
  ProcessGraph    <-one-to-many-> GraphEdge
  GraphEdge       <-one-to-two>   GraphNode
  Process         <-one-to-many-> ProcessInstance
  ProcessInstance <-one-to-many-> ExecutionEdge)

(def process-instance (instance/create process-v2))
