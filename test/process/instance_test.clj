(ns process.instance-test
  (:require [clojure.test :refer :all]
            [process.core-test :as fixtures]
            [process.instance :as instance]))

(def ^:private process-instance (instance/create fixtures/integration-process))

(deftest indexing-test
  (testing "Denormalized nodes are indexed correctly"
    (let [{:keys [nodes id->nodes]} process-instance]
      (is (= (into #{} nodes)
             (into #{} (vals id->nodes))))
      (is (every? (fn [[id node]]
                    (= id (:id node)))
                  id->nodes)))))

(deftest runtime-state-test
  (testing "Runtime state is initialized correctly"
    (let [{:keys [runtime-state]} process-instance]
      (is (instance? clojure.lang.IAtom runtime-state))
      (is (= {} (:context @runtime-state)))
      (is (= #{{:from 0 :to 1}}
             (:execution-edges @runtime-state))))))
