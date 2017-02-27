(ns process.execution
  "Process execution engine. Executes process instances, handling node
   transition functions, maintaining process history queue and support for
   asynchronous process execution."
  (:require [clojure.set :as s]
            [clojure.core.async :as async :refer [go <! close!]]))

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

(defn- transition-node!
  "Update process runtime state and fire off new executions according to node transition result."
  [{:keys [incoming] :as node} [outgoing-edges new-context actor] {:keys [runtime-state] :as process-instance}]
  (let [cleaned-context (clean-context new-context)
        actor (or actor ::process)]
    (swap! runtime-state
           (fn [state]
             (-> state
                 (update :context merge cleaned-context)
                 (update :execution-edges #(-> % (s/difference incoming) (into outgoing-edges)))
                 (update :process-history conj {:context cleaned-context
                                                :node node
                                                :actor actor}))))
    (if (:process.node.event/finished? new-context)
      (terminate-execution! process-instance)
      (doseq [edge outgoing-edges]
        (step-execution edge process-instance)))))

(defn- channel? [c]
  (instance? clojure.core.async.impl.protocols.Channel c))

(defn- go-async
  "Wait till the resume channel delivers transition update, restart execution after that"
  [node resume-channel process-instance]
  (go
    (when-let [transition-result (<! resume-channel)]
      (transition-node! node transition-result process-instance))))

(defn- step-execution
  "Recursively step through process nodes."
  [{:keys [from to] :as execution-edge}
   {:keys [id->nodes node-inputs runtime-state] :as process-instance}]
  (let [{:keys [execution-edges context]} @runtime-state]
    (when-not (:process.node.event/finished? context)
      (let [{:keys [transition id] :as node} (get id->nodes to)
            transition-result (transition (prepare-context context id node-inputs execution-edges))]
        (cond
          (channel? transition-result) (go-async node transition-result process-instance)
          transition-result (transition-node! node transition-result process-instance))))))

(defn execute-process-instance!
  "Runs execution on provided process-instance, returns runtime-state atom."
  [{:keys [runtime-state] :as process-instance}]
  (doseq [execution-edge (:execution-edges @runtime-state)]
    (step-execution execution-edge process-instance))
  runtime-state)
