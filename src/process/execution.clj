(ns process.execution
  (:require [clojure.core.async :as async :refer [go <! close!]]))

(defn- prepare-context
  [ctx node-id node-inputs execution-edges]
  (assoc ctx
         :pending-executions (into #{} (filter (comp (partial = node-id) :to)) execution-edges)
         :node-input (get node-inputs node-id)))

(defn- clean-context
  [ctx]
  (dissoc ctx :pending-executions :node-input))

(defn- terminate-execution!
  [{:keys [process-input] :as process-instance}]
  (close! process-input))

(declare step-execution)

(defn- proceed-executions!
  "Update runtime state and fire off new executions."
  [old-execution new-executions new-context {:keys [runtime-state] :as process-instance}]
  (swap! runtime-state
         (fn [state]
           (-> state
               (assoc :context (clean-context new-context))
               (update :execution-edges #(-> % (disj old-execution) (into new-executions))))))
  (if (:process.node.event/finished? new-context)
    (terminate-execution! process-instance)
    (doseq [edge new-executions]
      (step-execution edge process-instance))))

(defn- channel? [c]
  (instance? clojure.core.async.impl.protocols.Channel c))

(defn- go-async
  "Wait till the resume channel delivers transition update, restart execution after that"
  [resume-channel execution-edge process-instance]
  (go
    (when-let [[outgoing-edges updated-context] (<! resume-channel)]
      (proceed-executions! execution-edge outgoing-edges updated-context process-instance))))

(defn- step-execution
  [{:keys [from to] :as execution-edge}
   {:keys [id->nodes node-inputs runtime-state] :as process-instance}]
  (let [{:keys [execution-edges context]} @runtime-state]
    (when-not (:process.node.event/finished? context)
      (let [{:keys [transition id]} (get id->nodes to)
            transition-result (transition (prepare-context context id node-inputs execution-edges))]
        (if (channel? transition-result)
          (go-async transition-result execution-edge process-instance)
          (let [[outgoing-edges updated-context] transition-result]
            (proceed-executions! execution-edge outgoing-edges updated-context process-instance)))))))

(defn execute-process-instance!
  "Runs execution on provided process-instance, returns runtime-state atom."
  [{:keys [runtime-state] :as process-instance}]
  (doseq [execution-edge (:execution-edges @runtime-state)]
    (step-execution execution-edge process-instance))
  runtime-state)
