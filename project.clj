(defproject com.stuartsierra/flow "0.0.2-SNAPSHOT"
  :description "Function definitions derived from graph declarations"
  :url "https://github.com/stuartsierra/flow"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.namespace "0.2.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.4.0"]
                                  [org.clojure/java.classpath "0.2.0"]]
                   :source-paths ["dev"]}})

