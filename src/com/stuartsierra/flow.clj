;; Copyright (c) 2012 Stuart Sierra. All rights reserved. This program
;; and the accompanying materials are made available under the terms
;; of the Eclipse Public License v1.0 which accompanies this
;; distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html

(ns com.stuartsierra.flow
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]
            [clojure.java.io :as io]))

(comment
  ;; source-map structure:
  {sym3 ([sym1 sym2] body...)})

(defn symbol-graph
  "Returns the dependency graph of symbols in the flow source map."
  [source-map]
  (reduce (fn [g [output [[& inputs]]]]
            (reduce (fn [gg input]
                      (dep/depend gg output input))
                    g
                    inputs))
          (dep/graph)
          source-map))

(defrecord Flow [source-map fn-map graph])

(defmacro flow
  "Returns a Flow created from the given source map. The source map is
  composed of pairs like

      symbol ([params*] body*)

  params and body will be compiled into a function that returns the
  value of the symbol."
  [& pairs]
  (let [source-map (apply hash-map pairs)
        fn-map (reduce (fn [m [output [inputs & body]]]
                         (assoc m `'~output `(fn ~inputs ~@body)))
                       {} source-map)]
    `(->Flow '~source-map ~fn-map (symbol-graph '~source-map))))

(defn- call [sym flow results]
  (let [f (get (:fn-map flow) sym)
        [args] (get (:source-map flow) sym)]
    (assert f)
    (assert (every? #(contains? results %) args))
    (apply f (map results args))))

(defn- eval-order [flow input-syms output-syms]
  (let [required (apply set/union
                        (map #(dep/transitive-dependencies (:graph flow) %)
                             output-syms))]
    (sort (dep/topo-comparator (:graph flow))
          (concat required output-syms))))

(defn run
  "Dynamically executes a flow to compute the values of symbols in
  output-syms given an input-map from symbols to values. Returns a map
  from symbols to their computed values."
  ([flow input-map]
     (run flow input-map (keys (:fn-map flow))))
  ([flow input-map output-syms]
     (let [input-syms (keys input-map)]
       (loop [results input-map
              todo (eval-order flow input-syms output-syms)]
         (if (seq todo)
           (let [sym (first todo)]
             (cond (contains? results sym)
                   (recur results (rest todo))
                   (contains? (:fn-map flow) sym)
                   (recur (assoc results sym (call sym flow results))
                          (rest todo))
                   :else
                   (throw (ex-info (str "Missing value for " sym)
                                   {:symbol sym
                                    :output-syms output-syms
                                    :results results}))))
           results)))))

(defn- sorted-bindings [flow input-syms output-syms]
  (loop [let-bindings []
         results (set input-syms)
         todo (eval-order flow input-syms output-syms)]
    (if (seq todo)
      (let [sym (first todo)]
        (cond (contains? results sym)
              (recur let-bindings results (rest todo))
              (contains? (:source-map flow) sym)
              (recur (conj let-bindings
                           sym
                           (cons 'do (rest (get (:source-map flow) sym))))
                     (conj results sym)
                     (rest todo))))
      let-bindings)))

(defn- return-output-syms [return]
  (cond (symbol? return)
          [return]
        (map? return)
          (vals return)
        (sequential? return)
          return
        :else
          (throw (ex-info "Invalid return spec" {:return return}))))

(defmacro flow-fn
  "Returns a compiled function that computes values for all or part of
  a flow. argv is the argument vector to the returned function; it
  must be a vector of symbols defined in the flow. return is a symbol
  in the flow, a vector of symbols in the flow, or a map whose values
  are symbols in the flow; it will be the return value of the
  function.

  flow will be evaluated at compile-time."
  [flow argv return]
  {:pre [(vector? argv) (every? symbol? argv)]}
  (let [flow (eval flow)
        output-syms (return-output-syms return)]
    `(fn ~argv
       (let [~@(sorted-bindings flow argv output-syms)]
         ~return))))

(defmacro flow-map-fn
  "Returns a compiled function that computes all values for a flow.
  The function will take one argument, a map from symbols to values,
  and return a map with the same structure.

  flow will be evaluated at compile-time."
  [flow]
  (let [flow (eval flow)
        input-syms (filter #(not (contains? (:source-map flow) %))
                           (dep/nodes (:graph flow)))
        output-syms (keys (:source-map flow))
        all-syms (concat input-syms output-syms)]
    `(fn [{:syms ~(vec input-syms)}]
       (let [~@(sorted-bindings flow input-syms output-syms)]
         ~(zipmap (map #(list 'quote %) all-syms) all-syms)))))

(defn dot
  "Prints a representation of the flow to standard output,
  suitable for the Graphviz 'dot' program. graph-name is a symbol,
  must not be a Graphviz reserved word (such as 'graph')."
  [flow graph-name]
  (let [graph (:graph flow)]
    (println "digraph" graph-name "{")
    (doseq [sym (dep/nodes graph)
            dep (dep/immediate-dependents graph sym)]
      (println "  " sym '-> dep \;))
    (println "}")))

(defn write-dotfile
  "Writes a Graphviz dotfile for a Flow. graph-name is a symbol, must
  not be a Graphviz reserved-word (such as 'graph')."
  [flow graph-name file-name]
  (with-open [wtr (io/writer file-name)]
    (binding [*out* wtr]
      (dot flow graph-name))))

