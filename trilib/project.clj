(defproject trilib "0.6.0-SNAPSHOT"
  :description "Trilystro common code"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [expound "0.5.0"]
                 [fsmviz "0.1.3"]
                 [lambdaisland/uri "1.1.0"]
                 [re-frame "0.10.5"]
                 [re-frame-utils "0.1.0"]
                 [com.degel/iron "0.2.0"]
                 [com.degel/re-frame-firebase "0.5.0"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :compiler {:pretty-print true}}]})
