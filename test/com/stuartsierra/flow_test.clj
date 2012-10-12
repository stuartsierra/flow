(ns com.stuartsierra.flow-test
  (:use com.stuartsierra.flow
        clojure.test))

(def f1
  (flow c ([a] (+ a 2))
        d ([a b] (+ a b))
        e ([c d] (+ c d))))

(def get-e (flow-fn f1 [a b] e))

(def get-c (flow-fn f1 [a] {:c c}))

(def run-f1 (flow-map-fn f1))

(deftest run-complete
  (is (= '{e 6, c 3, d 3, a 1, b 2}
         (run f1 '{a 1 b 2} '(e)))))

(deftest run-partial-1
  (is (= '{c 3, a 1, b 2}
         (run f1 '{a 1 b 2} '(c)))))

(deftest run-partial-2
  (is (= '{c 3, d 3, a 1, b 2}
         (run f1 '{a 1 b 2} '(c d)))))

(deftest run-partial-3
  (is (= '{d 3, a 1, b 2}
         (run f1 '{a 1 b 2} '(d)))))

(deftest run-partial-4
  (is (= '{a 1, b 2}
         (run f1 '{a 1 b 2} '(a)))))

(deftest run-missing
  (is (thrown? Exception (run f1 '{a 1} '(d)))))

(deftest get-value-fn
  (is (= 6 (get-e 1 2))))

(deftest get-map-fn
  (is (= {:c 3} (get-c 1))))

(deftest run-map-fn
  (is (= '{e 6, d 3, c 3, b 2, a 1}
         (run-f1 {'a 1 'b 2}))))
