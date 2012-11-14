(ns user
  (:use clojure.tools.namespace.repl
        clojure.repl
        clojure.pprint)
  (:require [clojure.set :as set]
            [clojure.test :as test]
            [com.stuartsierra.flow :as flow]))

(defn reset []
  (let [result (refresh)]
    (when-not (= :ok result)
      (in-ns 'user)
      (intern *ns* 'reset reset))
    result))

(def flow-1
  {:c (with-meta (fn [{:keys [a b]}] (+ a b))
        {::flow/inputs #{:a :b}})
   :d (with-meta (fn [{:keys [b c]}] (+ b c))
        {::flow/inputs #{:b :c}})
   :e (with-meta (fn [{:keys [c d]}] (+ c d))
        {::flow/inputs #{:c :d}})})

(def flow-2
  (flow/flow
   :c ([a b] (+ a b))
   :d ([b c] (+ b c))
   :e ([c d] (+ c d))))

;; (def f1 (flow/flow-fn flow-1))

;; (def f2 (flow/flow-fn flow-2))

;; (def f1c (flow/compile com.stuartsierra.flow-test/f1 [:a :b]))
;; #'user/f1c
;; user> (dotimes [j 5] (time (dotimes [i 10000] (flow/run com.stuartsierra.flow-test/f1 {:a 1 :b 2}))))
;; "Elapsed time: 623.644 msecs"
;; "Elapsed time: 614.011 msecs"
;; "Elapsed time: 628.677 msecs"
;; "Elapsed time: 610.751 msecs"
;; "Elapsed time: 617.071 msecs"
;; nil
;; user> (dotimes [j 5] (time (dotimes [i 10000] (f1c {:a 1 :b 2}))))
;; "Elapsed time: 34.631 msecs"
;; "Elapsed time: 30.742 msecs"
;; "Elapsed time: 19.598 msecs"
;; "Elapsed time: 21.219 msecs"
;; "Elapsed time: 20.001 msecs"
;; nil

(comment
)
