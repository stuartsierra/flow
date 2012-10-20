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

(defn symbol->keyword [symbol]
  {:pre [(symbol? symbol)]}
  (keyword (name symbol)))

(defmacro flow-map [& pairs]
  {:pre [(even? (count pairs))]}
  (into {} (map (fn [[output [inputs & body]]]
                  {:pre [(keyword? output)
                         (vector? inputs)]}
                  [output `(with-meta
                             (fn [{:keys ~inputs}] ~@body)
                             {::inputs ~(set (map symbol->keyword inputs))})])
                (partition 2 pairs))))

(defn todo-keys [flow-map inputs outputs]
  (let [graph (flow-map-graph flow-map)
        comparator (dep/topo-comparator graph)
        work-graph (reduce dep/remove-all graph inputs)
        required (required-keys work-graph inputs outputs)]
    (sort comparator required)))

(defn run-flow-map
  ([flow-map input-map]
     (run-flow-map flow-map input-map (keys flow-map)))
  ([flow-map input-map outputs]
     (let [todo (todo-keys flow-map (keys input-map) outputs)]
       (prn :in 'run-flow-map :todo todo)
       (reduce (fn [output-map key]
                (if-let [f (get flow-map key)]
                  (assoc output-map key (f output-map))
                  (throw (ex-info (str "Missing value for " key)
                                  {:key key}))))
               input-map todo))))
