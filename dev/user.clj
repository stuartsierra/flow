;; I use this file for development and testing.

(ns user
  (:use clojure.tools.namespace.repl
        clojure.repl
        clojure.pprint)
  (:require [clojure.set :as set]
            [clojure.test :as test]
            [com.stuartsierra.flow :as flow]
            [com.stuartsierra.flow.component :as component]))

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

;;; examples from README:

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

(def process-flow-2
  (assoc process-flow
    :epsilon (flow/with-inputs [:alpha :beta]
               (fn [{:keys [alpha beta]}]
                 (+ (* 100 alpha) beta)))))

(def process-flow-2
  (assoc process-flow
    :epsilon (flow/flow-fn [alpha beta]
               (+ (* 100 alpha) beta))))



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

(flow/flow-let
   [alpha   ([] 1)
    beta    ([] 2)
    result  ([gamma delta epsilon]
               (subprocess-d gamma delta epsilon))
    gamma   ([alpha beta]  (subprocess-a alpha beta))
    delta   ([alpha gamma] (subprocess-b alpha gamma))
    epsilon ([gamma delta] (subprocess-c gamma delta))]
 (str "The result is " result))
;;=> "The result is 14"

)  ; end comment

(defn safe-prn [& args]
  (locking safe-prn
    (apply prn args)
    (flush)))

(defn dumb-component [name delay deps]
  (component/with-deps deps
    (reify component/Lifecycle
      (start [this]
        (future
          (safe-prn :starting name)
          (Thread/sleep delay)
          [:started name]))
      (stop [this]
        (future
          (safe-prn :stopping name)
          (Thread/sleep delay)
          [:stopped name])))))

(def components1
  {::a (dumb-component 'a 100 [])
   ::b (dumb-component 'b 200 [])
   ::c (dumb-component 'c 400 [::a ::b])
   ::d (dumb-component 'd 800 [::a ::b])
   ::e (dumb-component 'e 1600 [::b ::c])})
