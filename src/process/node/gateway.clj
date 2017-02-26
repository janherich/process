(ns process.node.gateway
  (:require [clojure.set :as set]
            [process.node :as node]))

(defn propagate-to-transition
  "Helper function which adapts gateway propagate function to generic node transition function."
  [propagate]
  (fn [ctx]
    (when-let [outgoing-edges (propagate ctx)]
      [outgoing-edges ctx])))

(defmethod node/expand :gateway/parallel [{:keys [incoming outgoing] :as node-spec}]
  (assoc node-spec :transition (propagate-to-transition
                                (fn [{:keys [pending-executions]}]
                                  (when (= pending-executions incoming)
                                    outgoing)))))

(defmethod node/expand :gateway/decision [{:keys [id incoming outgoing decisions] :as node-spec}]
  (when (or (not-every? set? (map first decisions))
            (not-every? ifn? (map second decisions)))
    (throw
     (ex-info "Decisions must be sequence of [node-set decision-fn] tuples"
              {:decisions decisions})))
  (-> node-spec
      (assoc :transition (propagate-to-transition
                          (fn [{:keys [pending-executions] :as ctx}]
                            (if-let [propagate (some (fn [[node-set decision?]]
                                                       (when (decision? ctx)
                                                         (into #{}
                                                               (map (fn [to]
                                                                      {:from id :to to}))
                                                               node-set)))
                                                     decisions)]
                              (do
                                (when-not (set/subset? propagate outgoing)
                                  (throw
                                   (ex-info "Gatway defines propagation to nodes not connected to gateway"
                                            {:propagate propagate
                                             :outgoing outgoing})))
                                propagate)
                              (let [propagate-set (into #{}
                                                        (mapcat (fn [[node-set]]
                                                                  (map (fn [to]
                                                                         {:from id :to to})
                                                                       node-set)))
                                                        decisions)]
                                (set/difference outgoing propagate-set))))))
      (dissoc :decisions)))
