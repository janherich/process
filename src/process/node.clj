(ns process.node)

(defmulti expand
  "Expands sparse node-spec into full node specification, mainly filling in the node-transition
   function, which takes process context as an argument and returns [outgoing-transitions context]
   tuple."
  (fn [{:keys [type transition] :as node-spec}]
    (if (and (not transition) type)
      type
      (when transition
        ::user-specified))))

(defmethod expand ::user-specified [{:keys [type] :as node-spec}]
  (if-not type
    ;; if node type is not defined, supply generic `::user-specified` placeholder
    (assoc node-spec :type ::user-specified)
    node-spec))
