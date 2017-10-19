;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.fsm
  "Enhanced finite state machines for re-frame. This is a work in progress but my
  goal is to achieve something more expressive than the basic FSMs described in
  http://blog.cognitect.com/blog/2017/8/14/restate-your-ui-creating-a-user-interface-with-re-frame-and-state-machines
  yet without the full complexity of state charts
  (http://www.wisdom.weizmann.ac.il/~harel/SCANNED.PAPERS/Statecharts.pdf).

  This module supports:
  - basic state transitions (here called :shift).
  - a stack of states: :push adds a state to the stack. :pop removes the top state from
    the stack. :shift changes the top state in the stack. (When the stack has only one
    element, this is just a basic state transition, of course).
  - Triggers: Cause another re-frame event to occur because of an FSM action. Triggers
    can be supplied in calls to shift, push or pop. They can also be specified in the
    FSM graph, to occur automatically whenever an action occurs.

  [TODO] This module will leave Trilystro and move into its own library once it has had
  a chance to mature a bit more."
  (:require
   [clojure.string :as str]
   [fsmviz.core :as fsmviz]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.re-utils :refer [>evt]]
   [sodium.utils :as utils]))


(defn- next-stack
  "Apply an action to a state stack"
  [stack action new-state]
  (when (and (second stack) (= action :shift))
    ;; [TODO] Decide if we really want to support this.
    (console :log "FSM shift occurred while in a pushed state. This is allowed
                  but violates decades of programming good taste. Are you sure?"))
  (case action
    :shift (conj (pop stack) new-state)
    :push  (conj stack new-state)
    :pop   (pop stack)
    [:error]))

(defn- transit-state!
  "This is the FSM manager. Perform an action on the stack, and dispatch any trigger.
   Note that a trigger is always a re-frame event vector. It can be supplied in
   the definition of the FSM graph (to be called each time a transition occurs) or can
   be passed in explicitly (to occur once). When both vectors are supplied, they are
   concatenated, effectively passing the one-time trigger as a parameter to the graph's
   trigger."
  [state-graph state-stack transition trigger-params]
  (let [top-state (peek state-stack)
        [action new-state trigger] (get-in state-graph [top-state transition])
        new-stack (next-stack state-stack action new-state)
        full-trigger (when (or trigger trigger-params)
                       (utils/vconcat trigger trigger-params))]
    ;; (console :log "PAGE TRANSITION: " state-stack transition new-stack)
    (when-not (empty? full-trigger)
      ;; (console :log "-> TRIGGERING: " full-trigger)
      (>evt full-trigger))
    new-stack))


(defn in-state?
  "Utility predicate to test if a state is active."
  [state-stack state]
  (-> #{state} (some state-stack) boolean))


;;; Trilystro app state

(def page-states
  "Trilystro top-level state.  Logged-in vs logged-out and several modal popups."
  {:start             {:initialize-db       [:shift :logged-out]}
   :logged-out        {:login-confirmed     [:shift :logged-in]
                       :logout              [:shift :logged-out]
                       :try-login           [:shift :logging-in]
                       :modal-about         [:push :modal-about]}
   :logging-in        {:firebase-error      [:shift :error]
                       :login-confirmed     [:shift :logged-in]
                       :logout              [:shift :logged-out]}
   :logged-in         {:login-confirmed     [:shift :logged-in]
                       :logout              [:shift :logged-out]
                       :modal-edit-lystro   [:push :modal-edit-lystro]
                       :modal-new-lystro    [:push :modal-new-lystro]
                       :modal-about         [:push :modal-about]}
   :modal-edit-lystro {:quit-modal          [:pop]}
   :modal-new-lystro  {:quit-modal          [:pop]}
   :modal-about       {:quit-modal          [:pop]}
   :error             {:error-handled       [:shift :start]}})

(def transit-page! (partial transit-state! page-states))

;; [TODO] This should be private. Fix callers
(defn page [db transition trigger-params]
  (update db :page-state transit-page! transition trigger-params))

(re-frame/reg-event-db
 :page
 (fn [db [_ transition trigger-params]]
   (page db transition trigger-params)))




;;; Graphing. (This is still a bit hackish, as it works around the existing fsmviz rather than modifying it).

(defn- nodes-pushing-into
  "Return the set of all nodes that push to a node.
  This is an attempt to graph the effects of pop. It is not complete, since it ignores
  potential non-structured usages such as {:push a; :shift b; :pop}. But, to be fair:
  I never have such usages; they violate 50 years of good taste in structured programming;
  and it may have been a mistake to even supporth this usage in next-stack."
  [graph node]
  (reduce-kv
   (fn [coll source edge-map]
     (into coll
           (reduce-kv
            (fn [sub-coll edge [type sink]]
              (if (and (= sink node) (= type :push))
                (conj sub-coll source)
                sub-coll))
            #{}
            edge-map)))
   #{}
   graph))


(defn- node-name
  "For some reason, fsmviz loses hyphens in node names"
  [key]
  (str/replace (name key) #"-" "_"))


(defn- graph-triple [type source edge sink]
  (let [edge-name (let [edge (name edge)]
                    (case type
                      :pop (str "POP " edge)
                      :push (str "PUSH " edge)
                      edge))]
    [(node-name source) edge-name (node-name sink)]))


(defn- flatten-graph
  "Convert state graph to flat form, as required by https://github.com/jebberjeb/fsmviz"
  [graph]
  (reduce-kv
   (fn [coll source edge-map]
     (into coll
           (reduce-kv
            (fn [sub-coll edge [type sink]]
              (if (= type :pop)
                (let [pushers (nodes-pushing-into graph source)]
                  (concat sub-coll (map (partial graph-triple type source edge) pushers)))
                (conj sub-coll (graph-triple type source edge sink))))
            []
            edge-map)))
   []
   graph))


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
