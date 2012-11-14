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
  (assoc f1 :c (constantly 100)))

(def f3
  (assoc f1 :c (flow-fn [a b] (+ 1000 a b))))

(def f4
  {:out/c (with-inputs #{:in/a}
            (fn [{a :in/a}] (+ a 10)))
   :out/d (with-inputs #{:in/b}
            (fn [{a :in/a b :in/b}] (+ a b)))})

(def f1-ab-all (compile f1 [:a :b]))
(def f1-ab-c (compile f1 [:a :b] [:c]))
(def f1-ab-cd (compile f1 [:a :b] [:c :d]))
(def f2-ab-all (compile f2 [:a :b]))
(def f3-ab-all (compile f3 [:a :b]))

(deftest run-complete
  (is (= {:f 7 :e 6, :c 3, :d 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2}))))

(deftest compiled-complete
  (is (= {:f 7 :e 6, :c 3, :d 3, :a 1, :b 2}
         (f1-ab-all {:a 1 :b 2}))))

(deftest run-partial-1
  (is (= {:c 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2} [:c]))))

(deftest compiled-partial-1
  (is (= {:c 3, :a 1, :b 2}
         (f1-ab-c {:a 1 :b 2}))))

(deftest run-partial-2
  (is (= {:c 3, :d 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2} [:c :d]))))

(deftest compiled-partial-2
  (is (= {:c 3, :d 3, :a 1, :b 2}
         (f1-ab-cd {:a 1 :b 2}))))

(deftest run-partial-provided-input
  (is (= {:a 1, :b 2}
         (run f1 {:a 1 :b 2} [:b]))))

(deftest run-partial-provided-output
  (is (= {:d 3, :a 1, :b 2}
         (run f1 {:a 1 :b 2 :d 3} [:d]))))

(deftest run-missing
  (is (thrown? Exception (run f1 {:a 1} [:d]))))

(deftest compiled-missing
  (is (thrown? Exception (f1-ab-all {:a 1}))))

(deftest run-const
  (is (= {:f 7 :e 103, :c 100, :d 3, :a 1, :b 2}
         (run f2 {:a 1 :b 2}))))

(deftest compiled-const
  (is (= {:f 7 :e 103, :c 100, :d 3, :a 1, :b 2}
         (f2-ab-all {:a 1 :b 2}))))

(deftest run-modified
  (is (= {:f 7 :e 1006, :c 1003, :d 3, :a 1, :b 2}
         (run f3 {:a 1 :b 2}))))

(deftest compiled-modified
  (is (= {:f 7 :e 1006, :c 1003, :d 3, :a 1, :b 2}
         (f3-ab-all {:a 1 :b 2}))))

(deftest t-flow-let
  (is (= {:a 3, :b 2}
         (flow-let [a ([b] (+ b 1))
                         b ([] 2)]
           {:a a :b b})))
  (is (= {:a 3, :b 2, :c 15}
         (flow-let [a ([b] (+ b 1))
                         b ([] 2)
                         c ([a b] (+ a b 10))]
           {:a a :b b :c c})))
  (is (= {:a 3, :b 2, :c 15}
         (flow-let [c ([a b] (+ a b 10))
                         a ([b] (+ b 1))
                         b ([] 2)]
           {:a a :b b :c c}))))

(deftest namespaced-keys
  (is (= {:out/c 11, :out/d 3, :in/b 2, :in/a 1}
         (run f4 {:in/a 1 :in/b 2}))))
