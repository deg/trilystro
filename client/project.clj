;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(defproject trilystro "0.4.1"
  :description "A toy app to explore Firebase"
  :url "https://github.com/deg/trilystro"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [cljs-ajax "0.7.3"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [compojure "1.6.0"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [expound "0.3.4"]
                 [garden "1.3.3"]
                 [ns-tracker "0.3.1"]
                 [re-frame "0.10.3-alpha1"]
                 [re-frame-utils "0.1.0"]
                 [reagent "0.7.0"]
                 [ring "1.6.3"]
                 [secretary "1.2.3"]
                 [yogthos/config "0.9"]
                 [com.degel/re-frame-firebase "0.5.0"]
                 [com.degel/iron "0.2.0"]
                 [com.degel/sodium "0.10.0"]
                 [trilib "0.4.1"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-garden "0.3.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"
                 "checkouts/iron/src"
                 "checkouts/re-frame-firebase/src"
                 "checkouts/sodium/src"
                 "checkouts/trilib/src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"
                                    "resources/public/css"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler trilystro.handler/dev-handler}

  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   trilystro.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.8"]
                   [figwheel-sidecar "0.5.14"]
                   [com.cemerick/piggieback "0.2.2"]
                   [re-frisk "0.5.3"]
                   ;[day8.re-frame/trace "0.1.13"]
                   ]

    :plugins      [[lein-figwheel "0.5.14"]
                   [lein-doo "0.1.8"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"
                    "checkouts/iron/src"
                    "checkouts/re-frame-firebase/src"
                    "checkouts/trilib/src"
                    "checkouts/sodium/src"]
     :figwheel     {:on-jsload "trilystro.core/mount-root"}
     :compiler     {:main                 trilystro.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :language-in :ecmascript5
                    ;:closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :preloads             [devtools.preload #_ day8.re-frame.trace.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            trilystro.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :language-in :ecmascript5
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :externs         ["externs.js"]
                    ;; [NOTE] When release build fails, uncomment these and set :pretty-print to true
                    ;; :pseudo-names true
                    ;; :verbose true
                    :pretty-print    false}}

    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:main          trilystro.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :language-in :ecmascript5
                    :optimizations :none}}
    ]}

  :main trilystro.server

  :aot [trilystro.server]

  :uberjar-name "trilystro.jar"

  :prep-tasks [["cljsbuild" "once" "min"]["garden" "once"] "compile"]
  )
