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

(def subprocess-a +)
(def subprocess-b +)
(def subprocess-c +)
(def subprocess-d +)

(def process-flow
  (flow/flow
   :result  ([gamma delta epsilon]
               (subprocess-d gamma delta epsilon))
   :gamma   ([alpha beta]  (subprocess-a alpha beta))
   :delta   ([alpha gamma] (subprocess-b alpha gamma))
   :epsilon ([gamma delta] (subprocess-c gamma delta))))

(def compute (flow/compile process-flow [:alpha :beta]))

(def compute-gamma
  (flow/compile process-flow [:alpha :beta] [:gamma]))

(comment

(flow/run process-flow {:alpha 1 :beta 2} [:result])
;;=> {:result 14, :epsilon 7, :delta 4, :gamma 3,
;;    :alpha 1, :beta 2}

(flow/run process-flow {:alpha 1 :beta 2} [:gamma :delta])
;;=> {:delta 4, :gamma 3, :alpha 1, :beta 2}

(flow/run process-flow {:alpha 1 :beta 2 :gamma 30} [:result])
;;=> {:result 122, :epsilon 61, :delta 31, :alpha 1,
;;    :beta 2, :gamma 30}

(compute {:alpha 1 :beta 2})
;;=> {:result 14, :epsilon 7, :delta 4, :gamma 3,
;;    :alpha 1, :beta 2}

(compute-gamma {:alpha 1 :beta 2})
;;=> {:gamma 3, :alpha 1, :beta 2}

)  ; end comment

