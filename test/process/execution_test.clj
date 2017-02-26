(ns process.execution-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [put! >!!]]
            [process.test-data :as fixtures]
            [process.instance :as instance]
            [process.execution :as execution]))

(defn- wait-for-context
  [ctx-fn runtime-state]
  (when-not (ctx-fn (:context @runtime-state))
    (Thread/sleep 500)
    (recur ctx-fn runtime-state)))

(def ^:private wait-for-finished (partial wait-for-context :process.node.event/finished?))

(deftest integration-process-path-1-test
  (let [process-instance (instance/create fixtures/integration-process)
        runtime-state (execution/execute-process-instance! process-instance)
        process-input (:process-input process-instance)]
    (testing "Stepping through process works correctly"
      (is (= {:execution-edges #{{:from 1 :to 2}}
              :context {:project-files-size 200}}
             (select-keys @runtime-state [:execution-edges :context])))
      (put! process-input {:node-id 2 :data true})
      (wait-for-finished runtime-state)
      (is (= {:execution-edges #{}
              :context {:project-files-size 200
                        :write-manifest? true
                        :manifest-written true
                        :process.node.event/finished? true}}
             (select-keys @runtime-state [:execution-edges :context]))))
    (testing "Process history is written in the queue"
      (is (= '(1 2 3 4 5)
             (map (comp :id :node) (:process-history @runtime-state)))))
    (testing "Process input channel is closed when process instance is finished"
      (is (not (put! process-input "put! returns false if channel is already closed"))))))

(deftest integration-process-path-2-test
  (let [process-instance (instance/create fixtures/integration-process)
        runtime-state (execution/execute-process-instance! process-instance)
        process-input (:process-input process-instance)]
    (testing "Stepping through process works correctly"
      (is (= {:execution-edges #{{:from 1 :to 2}}
              :context {:project-files-size 200}}
             (select-keys @runtime-state [:execution-edges :context])))
      (put! process-input {:node-id 2 :data false})
      (wait-for-finished runtime-state)
      (is (= {:execution-edges #{}
              :context {:project-files-size 200
                        :write-manifest? false
                        :process.node.event/finished? true}}
             (select-keys @runtime-state [:execution-edges :context]))))
    (testing "Process history is written in the queue"
      (is (= '(1 2 3 5)
             (map (comp :id :node) (:process-history @runtime-state)))))
    (testing "Process input channel is closed when process instance is finished"
      (is (not (put! process-input "put! returns false if channel is already closed"))))))

(deftest parallel-process-test
  (let [process-instance (instance/create fixtures/parallel-process)
        runtime-state (execution/execute-process-instance! process-instance)
        process-input (:process-input process-instance)]
    (testing "Splitting executions from parallel gateway works correctly"
      (is (= #{{:from 1 :to 2} {:from 1 :to 3}}
             (:execution-edges @runtime-state))))
    (testing "Parallel gateway will hold executions if not all incoming edges are pending"
      (put! process-input {:node-id 2 :data "Data 1"})
      (wait-for-context :data-1 runtime-state)
      (is (= #{{:from 1 :to 3} {:from 2 :to 4}}
             (:execution-edges @runtime-state))))
    (testing "Process context is written after process is finished"
      (put! process-input {:node-id 3 :data "Data 2"})
      (wait-for-finished runtime-state)
      (is (= {:data-1 "Data 1"
              :data-2 "Data 2"
              :process.node.event/finished? true}
             (:context @runtime-state))))
    (testing "Process history is written in the queue"
      (is (= '(1 2 3 4 5)
             (map (comp :id :node) (:process-history @runtime-state)))))))
