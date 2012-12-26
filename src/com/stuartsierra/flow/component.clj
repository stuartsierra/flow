(ns com.stuartsierra.flow.component
  "Components with managed lifecycles. A component is an object which
  implements the Lifecycle protocol defined here.

  Components are grouped together in a Component Map, which is a map
  from keys (usually keywords) to Component objects which have
  metadata specifying the keys of other components they depend on."
  (:require [clojure.tools.namespace.dependency :as dep]))

(defprotocol Lifecycle
  (start [this]
    "Begins operation of this component, possibly modifying its state.
  Returns a Future/Promise on which the caller can block to wait until
  this component is completely started. The value of the
  Future/Promise may include results of starting, but it is not
  necessarily the same as this object.")
  (stop [this]
    "Shuts down this component, gracefully, possibly modifying its state.
  Returns a Future/Promise on which the caller can block to wait until
  this component is completely stopped. The value of the
  Future/Promise may include results of stopping, but it is not
  necessarily the same as this object."))

(comment
  ;; component graph
  {:key ^{::dependencies #{:key1 :key2}} (Component)})

(defn- dependency-graph [component-map]
  (reduce-kv (fn [graph key component]
               (reduce (fn [g dep] (dep/depend g key dep))
                       graph (::dependencies (meta component))))
             (dep/graph) component-map))

(defn with-deps [dependency-keys component]
  (vary-meta component assoc ::dependencies (set dependency-keys)))

(defmacro ^:private try-do [key target op]
  `(let [target# ~target]
     (try (~op target#)
          (catch Throwable t#
            (throw (ex-info "Component operation failed"
                            {:key ~key
                             :target target#
                             :operation ~op
                             :in-progress ~'in-progress}
                            t#))))))

(defn- deref-all
  ([in-progress]
     (deref-all in-progress (keys in-progress)))
  ([in-progress keys]
     (reduce (fn [m k]
               (assoc m k (try-do k (get in-progress k) deref)))
             {} keys)))

(defn call-in-dependency-order
  [component-map f]
  (let [graph (dependency-graph component-map)]
    (->> (dep/topo-sort graph)
         (reduce (fn [in-progress key]
                   (deref-all in-progress
                              (dep/transitive-dependencies graph key))
                   (assoc in-progress key
                          (try-do key (get component-map key) f)))
                 {})
         deref-all)))

(defn call-in-dependent-order
  [component-map f]
  (let [graph (dependency-graph component-map)]
    (->> (dep/topo-sort graph)
         reverse
         (reduce (fn [in-progress key]
                   (deref-all in-progress
                              (dep/transitive-dependents graph key))
                   (assoc in-progress key
                          (try-do key (get component-map key) stop)))
                 {})
         deref-all)))

(defn start-all
  [component-map]
  (call-in-dependency-order component-map start))

(defn stop-all
  [component-map]
  (call-in-dependent-order component-map stop))

