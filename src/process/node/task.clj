(ns process.node.task
  "Task type nodes, representing external input/output of the process."
  (:require [clojure.core.async :as async :refer [go <!]]
            [process.node :as node]))

(defmethod node/expand :task/input [{:keys [context-key outgoing] :as node-spec}]
  (-> node-spec
      (assoc :transition (fn [{:keys [node-input] :as ctx}]
                           (go (let [input (<! node-input)]
                                 [outgoing (assoc ctx context-key input)]))))
      (dissoc :context-key)))

(defmethod node/expand :task/action [{:keys [action-fn outgoing] :as node-spec}]
  (-> node-spec
      (assoc :transition (fn [ctx]
                           [outgoing (action-fn ctx)]))
      (dissoc :action-fn)))
