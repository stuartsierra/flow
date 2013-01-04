;; Copyright (c) 2012 Stuart Sierra. All rights reserved. This program
;; and the accompanying materials are made available under the terms
;; of the Eclipse Public License v1.0 which accompanies this
;; distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html

(ns com.stuartsierra.flow
  (:refer-clojure :exclude (compile))
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]
            [clojure.java.io :as io]
            [lazymap.core :as lazy]))

(comment
  ;; structure of a flow
  {:output ^{::inputs #{:input1 :input2}}
              (fn [{:keys [input1 input2]}]
                ;; returns value for :output
                )})

(defn- flow-graph [flow]
  (reduce (fn [graph [output f]]
            (reduce (fn [g input] (dep/depend g output input))
                    graph (::inputs (meta f))))
          (dep/graph) flow))

(defn- required-keys [work-graph inputs outputs]
  (apply set/union
         (set/difference (set outputs) (set inputs))
         (map #(dep/transitive-dependencies work-graph %)
              outputs)))

(defn- sym->key [symbol]
  {:pre [(symbol? symbol)]}
  (keyword (name symbol)))

(defn- destructured-map-keys [m]
  (set/union
   (set (map sym->key (:keys m)))
   (set (:syms m))
   (set (map name (:strs m)))
   (set (vals (dissoc m :keys :syms :strs :or :as)))))

(defn with-inputs
  "Returns function f with metadata specifying it takes the given
  inputs, a collection of keywords."
  [input-keys f]
  (with-meta f {::inputs (set input-keys)}))

(defmacro flow-fn
  "Returns a function for use in a flow. The function will take a
  single map argument. inputs is either a destructuring map form or a
  vector of symbols to destructure as with {:keys [...]}." 
  [inputs & body]
  (let [input-structure (if (vector? inputs)
                          {:keys inputs}
                          inputs)]
    `(with-inputs ~(destructured-map-keys input-structure)
       (fn [~input-structure] ~@body))))

(defmacro flow
  "Returns a flow from pairs like:

      output (inputs body...)

  inputs is either a destructuring map form or a vector of symbols to
  destructure as with {:keys [...]}.
  "
  [& pairs]
  {:pre [(even? (count pairs))]}
  (into {} (map (fn [[output fntail]]
                  [output `(flow-fn ~@fntail)])
                (partition 2 pairs))))

(defn- todo-keys [flow inputs outputs]
  (let [graph (flow-graph flow)
        comparator (dep/topo-comparator graph)
        work-graph (reduce dep/remove-all graph inputs)
        required (required-keys work-graph inputs outputs)]
    (sort comparator required)))

(defn- run-flow [flow todo input-map]
  (reduce (fn [output-map key]
            (if-let [f (get flow key)]
              (assoc output-map key (f output-map))
              (throw (ex-info (str "Missing value for " key)
                              {:key key}))))
          input-map todo))

(defn- run-flow-lazy [flow todo input-map]
  (reduce (fn [output-map key]
            (if-let [f (get flow key)]
              (lazy/lazy-assoc output-map key (f output-map))
              (throw (ex-info (str "Missing value for " key)
                              {:key key}))))
          (lazy/create-lazy-map input-map)
          todo))

(defn run
  "Executes a flow using the given input map. Optional third argument
  is a collection of keywords desired in the output map; if not
  present defaults to all keys in the flow."
  ([flow input-map]
     (run flow input-map (keys flow)))
  ([flow input-map outputs]
     (let [todo (todo-keys flow (keys input-map) outputs)]
       (run-flow flow todo input-map))))

(defn run-lazy
  "Returns a lazy map of a flow using the given input map. Optional third argument
  is a collection of keywords desired in the output map; if not
  present defaults to all keys in the flow."
  ([flow input-map]
     (run-lazy flow input-map (keys flow)))
  ([flow input-map outputs]
     (let [todo (todo-keys flow (keys input-map) outputs)]
       (run-flow-lazy flow todo input-map))))

(defn compile
  "Returns a function which executes the flow, precomputing the steps
  necessary. The returned function will take a single argument, a map
  from keywords to values. inputs is the collection of keys which must
  be provided in that map. Optional third argument outputs is a
  collection of keywords desired in the output map; if not present
  defaults to all keys in the flow."
  ([flow inputs]
     (compile flow inputs (keys flow)))
  ([flow inputs outputs]
     (let [todo (todo-keys flow inputs outputs)]
       (fn [input-map]
         (run-flow flow todo input-map)))))

(defmacro flow-let
  "Evaluates body with local bindings. A binding is a pair like

      output-sym ([input-syms*] body*)

  Bindings may appear in any order. The input-syms for each body
  specify which other bindings the body depends on. Bindings will be
  executed in dependency order."
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  (let [expr-map (reduce (fn [m [output [_ & body]]]
                           (assoc m output (cons 'do body)))
                         {} (partition 2 bindings))
        graph (reduce (fn [graph [output [inputs & _]]]
                        (reduce #(dep/depend %1 output %2)
                                graph inputs))
                      (dep/graph) (partition 2 bindings))
        todo (dep/topo-sort graph)
        let-bindings (mapcat (fn [key]
                               [key (get expr-map key)])
                             todo)]
    `(let [~@let-bindings]
       ~@body)))

(defn dot
  "Prints a representation of the flow to standard output,
  suitable for input to the Graphviz 'dot' program. Options are
  key-value pairs from:

    :graph-name   string/symbol/keyword naming the graph. Must not be
                  a Graphiviz reserved word such as \"graph\"."
  [flow & options]
  (let [{:keys [graph-name]
         :or {graph-name "flow"}} options
        graph (flow-graph flow)]
    (println "digraph" (pr-str (name graph-name)) "{")
    (doseq [sym (dep/nodes graph)
            dep (dep/immediate-dependents graph sym)]
      (println "  " (pr-str (name sym)) "->" (pr-str (name dep)) ";"))
    (println "}")))

(defn write-dotfile
  "Writes a Graphviz dotfile for a Flow. options are the same as for
  'dot'."
  [flow file-name & options]
  (with-open [wtr (io/writer file-name)]
    (binding [*out* wtr]
      (apply dot flow options))))

