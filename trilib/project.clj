(defproject trilib "0.1.0-SNAPSHOT"
  :description "Trilystro common code"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-RC2"]
                 [org.clojure/clojurescript "1.9.946"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :compiler {:pretty-print true}}]})
