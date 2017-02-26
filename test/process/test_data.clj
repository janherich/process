(ns process.test-data)

(def integration-process
  {:name "Run integration"
   :created #inst "2017-02-16T21:42:57"
   :version 1
   :doc "First version of the process"
   :process-graph {:nodes #{{:id 0
                             :type :event/start}
                            {:id 1
                             :name "List project files"
                             :type :task/action
                             :action-fn (fn [ctx]
                                          ;; faking the files listing call
                                          (assoc ctx :project-files-size 200))}
                            {:id 2
                             :name "Write manifest ?"
                             :type :task/input
                             :context-key :write-manifest?}
                            {:id 3
                             :name "Write manifest ?"
                             :type :gateway/decision
                             :decisions [[#{4} :write-manifest?]]}
                            {:id 4
                             :name "Write manifest"
                             :type :task/action
                             :action-fn (fn [{:keys [project-files-size] :as ctx}]
                                          ;; faking the manifest write
                                          (println "Writing manifest")
                                          (assoc ctx :manifest-written true))}
                            {:id 5
                             :type :event/end}}
                   :edges #{{:from 0 :to 1}
                            {:from 1 :to 2}
                            {:from 2 :to 3}
                            {:from 3 :to 4}
                            {:from 3 :to 5}
                            {:from 4 :to 5}}}})

(def parallel-process
  {:name "Parallel collect data"
   :created #inst "2017-02-26T20:34:33.289-00:00"
   :version 1
   :doc "First version of the process"
   :process-graph {:nodes #{{:id 0
                             :type :event/start}
                            {:id 1
                             :type :gateway/parallel}
                            {:id 2
                             :name "Collect data 1"
                             :type :task/input
                             :context-key :data-1}
                            {:id 3
                             :name "Collect data 2"
                             :type :task/input
                             :context-key :data-2}
                            {:id 4
                             :type :gateway/parallel}
                            {:id 5
                             :type :event/end}}
                   :edges #{{:from 0 :to 1}
                            {:from 1 :to 2}
                            {:from 1 :to 3}
                            {:from 2 :to 4}
                            {:from 3 :to 4}
                            {:from 4 :to 5}}}})
