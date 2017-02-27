(ns process.node.event
  "Event type nodes, such as start/end of the process."
  (:require [process.node :as node]))

(defmethod node/expand :event/start [{:keys [outgoing] :as node-spec}]
  (assoc node-spec :transition (fn [ctx] [outgoing ctx])))

(defmethod node/expand :event/end [node-spec]
  (assoc node-spec :transition (fn [ctx] [#{} (assoc ctx ::finished? true)])))
