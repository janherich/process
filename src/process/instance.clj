(ns process.instance
  "Creating/initializing process instances."
  (:require [clojure.core.async :as async :refer [chan pub sub]]
            [process.graph :as graph]))

(defn- node-inputs
  "Generate input channels for every node which is not `:start` or `:end` type,
   connected through pub/sub to process input channel"
  [process-input nodes]
  (let [subscription (pub process-input :node-id)]
    (transduce (comp (filter (comp not #{:event/start :event/end} :type))
                     (map :id))
               (completing
                (fn [acc task-id]
                  (let [task-channel (chan 1 (map #(select-keys % [:data :actor])))]
                    (sub subscription task-id task-channel)
                    (assoc acc task-id task-channel))))
               {}
               nodes)))

(defn- initialize-execution-edges
  "Initialize execution edges for every `:start` type node"
  [nodes]
  (into #{}
        (comp (filter (comp (partial = :event/start) :type))
              (mapcat :outgoing))
        nodes))

(defn create
  "Create process instance, normalizing data, initializing input channels, execution-edges
   and process history queue."
  [{:keys [process-graph] :as process-definition}]
  (let [process-input (chan)
        nodes (graph/de-normalize process-graph)]
    {:id (java.util.UUID/randomUUID)
     :process process-definition
     :nodes nodes
     :id->nodes (into {} (map (juxt :id identity)) nodes)
     :node-inputs (node-inputs process-input nodes)
     :process-input process-input
     :runtime-state (atom {:context {}
                           :execution-edges (initialize-execution-edges nodes)
                           :process-history clojure.lang.PersistentQueue/EMPTY})}))
