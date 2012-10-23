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
  ;; flow-map structure
  {:output ^{::inputs #{:input1 :input2}}
              (fn [map] ...returns output...)}
  )

(defn- valid-flow-map? [flow-map]
  (when-not (map? flow-map)
    (throw (ex-info "flow-map must be a map" {:flow-map flow-map})))
  (doseq [[k f] flow-map]
    (when-not (keyword? k)
      (throw (ex-info "flow-map keys must be keywords" {:key k})))
    (when-not (fn? f)
      (throw (ex-info "flow-map values must be functions"
                      {:value f})))
    (when-not (::inputs (meta f))
      (throw (ex-info "flow-map functions must have ::inputs metadata"
                      {:meta (meta f)}))))
  true)

(defn- flow-map-graph [flow-map]
  (reduce
   (fn [graph [k f]]
     (reduce #(dep/depend %1 k %2) graph (::inputs (meta f))))
   (dep/graph) flow-map))

(defn- required-keys [work-graph inputs outputs]
  (apply set/union
         (set/difference (set outputs) (set inputs))
         (map #(dep/transitive-dependencies work-graph %)
              outputs)))

(defn- keyword [symbol]
  {:pre [(symbol? symbol)]}
  (clojure.core/keyword (name symbol)))

(defmacro flow-map-fn
  "Returns a function suitable for use in a flow-map. The function will
  take a single map argument, which will be destructured as with
  {:keys []}. inputs is a vector of symbols to destructure out of the
  map."
  [inputs & body]
  {:pre [(vector? inputs)
         (every? symbol? inputs)]}
  `(with-meta
     (fn [{:keys ~inputs}] ~@body)
     {::inputs ~(set (map keyword inputs))}))

(defn flow-map-const [output]
  (with-meta (constantly output)
    {::inputs #{}}))

(defmacro flow-map
  "Returns a flow-map from pairs like:

      :output-key ([inputs] body...)
  "
  [& pairs]
  {:pre [(even? (count pairs))]}
  (into {} (map (fn [[output fntail]]
                  {:pre [(keyword? output)]}
                  [output `(flow-map-fn ~@fntail)])
                (partition 2 pairs))))

(defn- todo-keys [flow-map inputs outputs]
  (let [graph (flow-map-graph flow-map)
        comparator (dep/topo-comparator graph)
        work-graph (reduce dep/remove-all graph inputs)
        required (required-keys work-graph inputs outputs)]
    (sort comparator required)))

(defn- run-flow-map [flow-map todo input-map]
  (reduce (fn [output-map key]
            (if-let [f (get flow-map key)]
              (assoc output-map key (f output-map))
              (throw (ex-info (str "Missing value for " key)
                              {:key key}))))
          input-map todo))

(defn run
  "Executes a flow-map using the given input map. Optional third
  argument is a collection of keywords desired in the output map; if
  not present defaults to all keys in the flow-map."
  ([flow-map input-map]
     (run flow-map input-map (keys flow-map)))
  ([flow-map input-map outputs]
     {:pre [(valid-flow-map? flow-map)]}
     (let [todo (todo-keys flow-map (keys input-map) outputs)]
       (run-flow-map flow-map todo input-map))))

(defn compile
  "Returns a function which executes the flow-map. The returned
  function will take a single argument, a map containing the keys in
  inputs. Optional third argument is a collection of keywords desired
  in the output map; if not present defaults to all keys in the
  flow-map."
  ([flow-map inputs]
     (compile flow-map inputs (keys flow-map)))
  ([flow-map inputs outputs]
     (let [todo (todo-keys flow-map inputs outputs)]
       (fn [input-map]
         (run-flow-map flow-map todo input-map)))))
