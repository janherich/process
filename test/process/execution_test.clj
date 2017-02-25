(ns process.execution-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [put! <!!]]
            [process.core-test :as fixtures]
            [process.instance :as instance]
            [process.execution :as execution]))

(def ^:private process-instance (instance/create fixtures/integration-process))

(defn- wait-for-finished
  [runtime-state]
  (when-not (:process.node.event/finished? (:context @runtime-state))
    (Thread/sleep 500)
    (recur runtime-state)))

(deftest process-step-test
  (let [runtime-state (execution/execute-process-instance! process-instance)
        process-input (:process-input process-instance)]
    (testing "Stepping through process works correctly"
      (is (= {:execution-edges #{{:from 1 :to 2}}
              :context {:project-files-size 200}}
             @runtime-state))
      (put! process-input {:node-id 2 :data true})
      (wait-for-finished runtime-state)
      (is (= {:execution-edges #{}
              :context {:project-files-size 200
                        :write-manifest? true
                        :manifest-written true
                        :process.node.event/finished? true}}
             @runtime-state)))
    (testing "Process input channel is closed when process instance is finished"
      (is (not (put! process-input "put! returns false if channel is already closed"))))))
