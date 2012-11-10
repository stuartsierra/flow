;; Copyright (c) 2012 Stuart Sierra. All rights reserved. This program
;; and the accompanying materials are made available under the terms
;; of the Eclipse Public License v1.0 which accompanies this
;; distribution, and is available at
;; http://www.eclipse.org/legal/epl-v10.html

(ns com.stuartsierra.flow
  (:refer-clojure :exclude (keyword compile))
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dependency :as dep]
            [clojure.java.io :as io]))

(comment
  ;; structure of a flow
  {:output ^{::inputs #{:input1 :input2}}
              (fn [{:keys [input1 input2]}]
                ;; returns value for :output
                )})

(defn- flow-graph [flow]
  (reduce
   (fn [graph [k f]]
     (reduce #(dep/depend %1 k %2) graph (::inputs (meta f))))
   (dep/graph) flow))

(defn- required-keys [work-graph inputs outputs]
  (apply set/union
         (set/difference (set outputs) (set inputs))
         (map #(dep/transitive-dependencies work-graph %)
              outputs)))

(defn- keyword [symbol]
  {:pre [(symbol? symbol)]}
  (clojure.core/keyword (name symbol)))

(defn with-inputs
  "Returns function f with metadata specifying it takes the given
  inputs, a collection of keywords."
  [input-keys f]
  (with-meta f {::inputs (set input-keys)}))

(defmacro flow-fn
  "Returns a function suitable for use in a flow. The function will
  take a single map argument, which will be destructured as with
  {:keys []}. inputs is a vector of symbols to destructure out of the
  map."
  [inputs & body]
  {:pre [(vector? inputs)
         (every? symbol? inputs)]}
  `(with-inputs ~(set (map keyword inputs))
     (fn [{:keys ~inputs}] ~@body)))

(defmacro flow
  "Returns a flow from pairs like:

      :output-key ([inputs] body...)
  "
  [& pairs]
  {:pre [(even? (count pairs))]}
  (into {} (map (fn [[output fntail]]
                  {:pre [(keyword? output)]}
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

(defn run
  "Executes a flow using the given input map. Optional third
  argument is a collection of keywords desired in the output map; if
  not present defaults to all keys in the flow."
  ([flow input-map]
     (run flow input-map (keys flow)))
  ([flow input-map outputs]
     (let [todo (todo-keys flow (keys input-map) outputs)]
       (run-flow flow todo input-map))))

(defn compile
  "Returns a function which executes the flow. The returned function
  will take a single argument, a map from keywords to values. The
  argument 'inputs' is the collection of keys which must be provided
  in that map. Optional third argument is a collection of keywords
  desired in the output map; if not present defaults to all keys in
  the flow."
  ([flow inputs]
     (compile flow inputs (keys flow)))
  ([flow inputs outputs]
     (let [todo (todo-keys flow inputs outputs)]
       (fn [input-map]
         (run-flow flow todo input-map)))))
