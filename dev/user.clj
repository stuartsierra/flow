(ns user
  (:use clojure.tools.namespace.repl
        clojure.repl
        clojure.pprint
        com.stuartsierra.flow)
  (:require [clojure.test :as test]))

(def f1
  (flow
   c ([a b] (+ a b))
   d ([b c] (+ b c))
   e ([c d] (+ c d))))

(def f1-map (flow-map-fn f1))

(comment ;; performance comparison:
  user> (dotimes [_ 5] (time (dotimes [_ 10000] (run f1 {'a 1 'b 2}))))
  "Elapsed time: 1224.959 msecs"
  "Elapsed time: 702.149 msecs"
  "Elapsed time: 648.85 msecs"
  "Elapsed time: 650.455 msecs"
  "Elapsed time: 654.619 msecs"
  nil
  user> (dotimes [_ 5] (time (dotimes [_ 10000] (f1-map {'a 1 'b 2}))))
  "Elapsed time: 58.975 msecs"
  "Elapsed time: 18.597 msecs"
  "Elapsed time: 19.251 msecs"
  "Elapsed time: 20.185 msecs"
  "Elapsed time: 17.137 msecs"
  nil
  )