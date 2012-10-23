;; Copyright (c) 2012 Stuart Sierra. All rights reserved. This program
;; and the accompanying materials are made available under the terms
;; of the Eclipse Public License v1.0 which accompanies this
;; distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html

(ns com.stuartsierra.flow-test
  (:refer-clojure :exclude (compile))
  (:use com.stuartsierra.flow
        clojure.test))

(def f1
  (flow
   :c ([a] (+ a 2))
   :d ([a b] (+ a b))
   :e ([c d] (+ c d))
   :f ([] 7)))

(def f2
  (assoc f1 :c (const 100)))

(def f3
  (assoc f1 :c (flow-fn [a b] (+ 1000 a b))))

(deftest run-complete
  (is (= {:f 7 :e 6, :c 3, :d 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2}))))

(deftest run-partial-1
  (is (= {:c 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2} [:c]))))

(deftest run-partial-2
  (is (= {:c 3, :d 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2} [:c :d]))))

(deftest run-partial-provided-input
  (is (= {:a 1, :b 2}
         (run f1 {:a 1 :b 2} [:b]))))

(deftest run-partial-provided-output
  (is (= {:d 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2 :d 3} [:d]))))

(deftest run-missing
  (is (thrown? Exception (run f1 {:a 1} [:d]))))

(deftest run-const
  (is (= {:f 7 :e 103, :c 100, :d 3, :a 1, :b 2}
         (run f2 {:a 1 :b 2}))))

(deftest run-modified
  (is (= {:f 7 :e 1006, :c 1003, :d 3, :a 1, :b 2}
         (run f3 {:a 1 :b 2}))))