(ns process.graph
  (:require [process.node :as node]
            [process.node.task :as task]
            [process.node.event :as event]
            [process.node.gateway :as gateway]))

(defn- filter-edge-set
  [direction node-id edges]
  {:pre [(#{:to :from} direction)]}
  (into #{} (filter (comp (partial = node-id) direction)) edges))

(defn de-normalize
  "De-normalizes process graph graph structure, connecting edges to nodes
   and expanding node specifications."
  [{:keys [nodes edges] :as process-graph}]
  (into #{}
        (map (fn [{:keys [id] :as node-spec}]
               (-> node-spec
                   (assoc :incoming (filter-edge-set :to id edges))
                   (assoc :outgoing (filter-edge-set :from id edges))
                   node/expand)))
        nodes))
