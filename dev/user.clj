(ns user
  (:use clojure.tools.namespace.repl
        clojure.repl
        clojure.pprint)
  (:require [clojure.test :as test]
            [com.stuartsierra.flow :as flow]))

(defn reset []
  (let [result (refresh)]
    (when-not (= :ok result)
      (in-ns 'user)
      (intern *ns* 'reset reset))
    result))

(def flow-map-1
  {:c (with-meta (fn [{:keys [a b]}] (+ a b))
        {::flow/inputs #{:a :b}})
   :d (with-meta (fn [{:keys [b c]}] (+ b c))
        {::flow/inputs #{:b :c}})
   :e (with-meta (fn [{:keys [c d]}] (+ c d))
        {::flow/inputs #{:c :d}})})

(def flow-map-2
  (flow/flow-map
   :c ([a b] (+ a b))
   :d ([b c] (+ b c))
   :e ([c d] (+ c d))))

;; (def f1 (flow/flow-map-fn flow-map-1))

;; (def f2 (flow/flow-map-fn flow-map-2))