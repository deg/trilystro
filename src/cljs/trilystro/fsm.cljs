;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.fsm
  (:require
   [clojure.string :as str]
   [fsmviz.core :as fsmviz]
   [re-frame.loggers :refer [console]]))


(def page-states
  {:start             {:initialize-db       [:shift :logged-out]}
   :logged-out        {:login-confirmed     [:shift :logged-in]
                       :logout              [:shift :logged-out]
                       :try-login           [:shift :logging-in]}
   :logging-in        {:firebase-error      [:shift :error]
                       :login-confirmed     [:shift :logged-in]
                       :logout              [:shift :logged-out]}
   :logged-in         {:login-confirmed     [:shift :logged-in]
                       :logout              [:shift :logged-out]
                       :modal-edit-lystro   [:push :modal-edit-lystro]
                       :modal-new-lystro    [:push :modal-new-lystro]
                       :modal-about         [:push :modal-about]}
   :modal-edit-lystro {:quit                [:pop]}
   :modal-new-lystro  {:quit                [:pop]}
   :modal-about       {:quit                [:pop]}
   :error             {:error-handled       [:shift :start]}})



(defn transit-state [state-stack transition]
  (let [top-state (last state-stack)
        [action new-state] (get-in page-states [top-state transition])
        new-stack (case action
                    :shift (conj (pop state-stack) new-state)
                    :push  (conj state-stack new-state)
                    :pop   (pop state-stack)
                    [:error])]
    ;; (console :log "PAGE TRANSITION: " state-stack transition new-stack)
    new-stack))

(defn in-state? [state-stack state]
  (-> #{state} (some state-stack) boolean))

(defn- flatten-graph
  "Convert state graph to flat form, as required by https://github.com/jebberjeb/fsmviz"
  [graph]
  (reduce-kv
   (fn [coll source edge-map]
     (into coll
           (reduce-kv
            (fn [sub-coll edge [type sink]]
              (let [sink (or sink :logged-in)] ;; [TODO] This is tmp kludge to handle :pop transitions
                (conj sub-coll [(name source) (name edge) (name sink)])))
            []
            edge-map)))
   []
   graph))

;; [TODO] Move this into a re-frame sub
(defn render-graph
  "generate-image returns the SVG as an XML string with an embedded explicit width
  and height. Here we use an ugly hack to strip this info, and then embed it into
  hiccup for reagent."
  [graph]
  [:div {:dangerouslySetInnerHTML
         {:__html (-> graph
                      flatten-graph
                      (fsmviz.core/generate-image "fsm")
                      (str/replace-first #"width=\"\d*pt\""  "")
                      (str/replace-first #"height=\"\d*pt\"" ""))}}])
