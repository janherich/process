(ns process.graph-test
  (:require [clojure.test :refer :all]
            [process.test-data :as fixtures]
            [process.graph :as graph]))

(def ^:private de-normalized (graph/de-normalize fixtures/integration-process))

(deftest nodes-test
  (testing "Every node has a transition function after denormalization"
    (is (every? (comp fn? :transition) de-normalized)))
  (testing "Every node has a `:type` key present"
    (is (every? :type de-normalized))))

(deftest edges-test
  (testing "Every incoming/outgoing edge connected to node is part of the original edge set"
    (let [{:keys [edges]} (:process-graph fixtures/integration-process)]
      (is (every? (comp edges :incoming) de-normalized))
      (is (every? (comp edges :outgoing) de-normalized)))))
