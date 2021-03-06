;; Licensed to the Apache Software Foundation (ASF) under one
;; or more contributor license agreements.  See the NOTICE file
;; distributed with this work for additional information
;; regarding copyright ownership.  The ASF licenses this file
;; to you under the Apache License, Version 2.0 (the
;; "License"); you may not use this file except in compliance
;; with the License.  You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
(ns org.apache.storm.messaging-test
  (:use [clojure test])
  (:import [org.apache.storm.testing CompleteTopologyParam MockedSources TestWordCounter TestWordSpout TestGlobalCount TestEventLogSpout TestEventOrderCheckBolt])
  (:use [org.apache.storm config])
  (:import [org.apache.storm Testing Thrift LocalCluster$Builder])
  (:import [org.apache.storm.utils Utils]))

(deftest test-local-transport
  (doseq [transport-on? [false true]]
    (with-open [cluster (.build (doto (LocalCluster$Builder.)
                                  (.withSimulatedTime)
                                  (.withSupervisors 1)
                                  (.withPortsPerSupervisor 2)
                                  (.withDaemonConf {TOPOLOGY-WORKERS 2
                                                    STORM-LOCAL-MODE-ZMQ 
                                                    (if transport-on? true false) 
                                                    STORM-MESSAGING-TRANSPORT 
                                                    "org.apache.storm.messaging.netty.Context"})))]
      (let [topology (Thrift/buildTopology
                       {"1" (Thrift/prepareSpoutDetails
                              (TestWordSpout. true) (Integer. 2))}
                       {"2" (Thrift/prepareBoltDetails
                              {(Utils/getGlobalStreamId "1" nil)
                               (Thrift/prepareShuffleGrouping)}
                              (TestGlobalCount.) (Integer. 6))
                        })
            results (Testing/completeTopology cluster
                                       topology
                                       (doto (CompleteTopologyParam.)
                                         ;; important for test that
                                         ;; #tuples = multiple of 4 and 6
                                         (.setMockedSources (MockedSources. {"1" [["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ["a"] ["b"]
                                                           ]}
                                       ))))]
        (is (= (* 6 4) (.size (Testing/readTuples results "2"))))))))
