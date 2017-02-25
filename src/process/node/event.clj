(ns process.node.event
  (:require [process.node :as node]))

(defmethod node/expand :event/start [{:keys [outgoing] :as node-spec}]
  (assoc node-spec :transition (fn [ctx] [outgoing ctx])))

(defmethod node/expand :event/end [node-spec]
  (assoc node-spec :transition (fn [ctx] [#{} (assoc ctx ::finished? true)])))
