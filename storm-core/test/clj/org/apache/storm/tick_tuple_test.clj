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
(ns org.apache.storm.tick-tuple-test
  (:use [clojure test])
  (:use [org.apache.storm config])
  (:use [org.apache.storm.internal clojure])
  (:import [org.apache.storm Thrift LocalCluster$Builder])
  (:import [org.apache.storm.utils Utils]))

(defbolt noop-bolt ["tuple"] {:prepare true}
  [conf context collector]
  (bolt
   (execute [tuple])))

(defspout noop-spout ["tuple"]
  [conf context collector]
  (spout
   (nextTuple [])))

(deftest test-tick-tuple-works-with-system-bolt
    (with-open [cluster (.build (doto (LocalCluster$Builder.)
                                    (.withSimulatedTime)))]
    (let [topology (Thrift/buildTopology
                    {"1" (Thrift/prepareSpoutDetails noop-spout)}
                    {"2" (Thrift/prepareBoltDetails
                           {(Utils/getGlobalStreamId "1" nil)
                            (Thrift/prepareFieldsGrouping ["tuple"])}
                           noop-bolt)})]
      (try
        (.submitTopology cluster
                               "test"
                               {TOPOLOGY-TICK-TUPLE-FREQ-SECS 1}
                               topology)
        (.advanceClusterTime cluster 2)
        ;; if reaches here, it means everything works ok.
        (is true)
        (catch Exception e
          (is false))))))



