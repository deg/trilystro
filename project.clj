(defproject trilystro "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [reagent "0.6.0"]
                 [re-frame "0.9.4"]
                 [re-frisk "0.4.5"]
                 [secretary "1.2.3"]
                 [garden "1.3.2"]
                 [ns-tracker "0.3.0"]
                 [compojure "1.5.0"]
                 [yogthos/config "0.8"]
                 [ring "1.4.0"]

                 [degree9/firebase-cljs "1.3.0"]

                 ;; [BUG ALERT]
                 ;; The next line should not be needed.
                 ;;
                 ;; It shouldn't be here, since firebase-cljs already depends
                 ;; on cljsjs/firebase. But, without it, Emacs C-c M-J
                 ;; (M-x cider-jack-in-clojurescript) fails: cljsjs/firebase
                 ;; is downloaded ok to ~/.m2, but then it gives a warning
                 ;; "Could not find artifact cljsjs:firebase:jar:3.2.1-0 in central
                 ;;  (http://repo.maven.apache.org/maven2)" and dies
                 ;; Strangely, `lein figwheel dev` from a terminal runs fine.
                 ;;
                 ;; To duplicate the problem:
                 ;;
                 ;; - Clone this repo and comment out [cljsjs/firebase "3.2.1-0"].
                 ;; - lein clean
                 ;; - rm ~/.m2/repository/cljsjs/firebase/
                 ;; - Open emacs buffer on src/cljs/trilystro/events.cljs
                 ;; - C-c M-J
                 ;; * FAILURE
                 ;; * But:
                 ;; - Open terminal
                 ;; - lein figwheel dev
                 ;; * SUCCESS
                 ;;
                 ;; There is some discussion of this issue at
                 ;; https://clojurians-log.clojureverse.org/clojurescript/2017-08-15.html
                 ;; (search for user "deg") and a gist of the error at
                 ;; https://gist.github.com/deg/651b7b37ae5943b341e7868fc92590bf
                 ;;
                 ;; Background:
                 ;; - This project is a new, still vanilla lein-based project. It
                 ;;   was started with
                 ;;   `lein new re-frame trilystro +cider +test +handler +garden +routes +re-frisk`
                 ;;   and had only trivial additions.
                 ;; - degree/firebase-cljs also appears not complicated, but is built
                 ;;   with boot.
                 ;; - cljsjs/firebase also appears to be straightforward. And, I
                 ;;   have duplicated the problem even after substituting different
                 ;;   versions of it in the firebase-cljs project
                 ;;
                 ;; I'm at a loss as to what's gone wrong, but it appears to be somehow
                 ;; dependent on Cider, and maybe also on lein vs boot interactions.
                 ;; I'm currently running Cider 20170729.133.
                 ;;
                 [cljsjs/firebase "3.2.1-0"]

                 ]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-garden "0.2.8"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

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
   {:dependencies [[binaryage/devtools "0.8.2"]
                   [figwheel-sidecar "0.5.9"]
                   [com.cemerick/piggieback "0.2.1"]]

    :plugins      [[lein-figwheel "0.5.9"]
                   [lein-doo "0.1.7"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "trilystro.core/mount-root"}
     :compiler     {:main                 trilystro.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            trilystro.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}

    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:main          trilystro.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none}}
    ]}

  :main trilystro.server

  :aot [trilystro.server]

  :uberjar-name "trilystro.jar"

  :prep-tasks [["cljsbuild" "once" "min"]["garden" "once"] "compile"]
  )
