;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilib.fsm-lib
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
  - Parameters: At runtime, each state has an associated vector of parameters. This is
    useful particularly for passing parameters in :push transitions.
  - Triggers: Cause another re-frame event to occur because of an FSM action. Triggers
    can be supplied in calls to shift, push or pop. They can also be specified in the
    FSM graph, to occur automatically whenever an action occurs.

  [TODO] This module will leave Trilystro and move into its own library once it has had
  a chance to mature a bit more.

  [TODO] Look at https://github.com/nodename/stately, for good ideas to include here. "
  (:require
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [iron.re-utils :refer [>evt]]
   [iron.utils :as utils]))


(defn- next-stack
  "Apply an action to a state stack"
  [stack action new-state param]
  (when (and (second stack) (= action :shift))
    ;; [TODO] Decide if we really want to support this.
    (console :log "FSM shift occurred while in a pushed state. This is allowed
                  but violates decades of programming good taste. Are you sure?"))
  (case action
    :shift (conj (pop stack) [new-state param])
    :push  (conj stack [new-state param])
    :pop   (pop stack)
    nil))

(defn transit-state!
  "This is the FSM manager. Perform an action on the stack, and dispatch any trigger.
   Note that a trigger is always a re-frame event vector. It can be supplied in
   the definition of the FSM graph (to be called each time a transition occurs) or can
   be passed in explicitly (to occur once). When both vectors are supplied, they are
   concatenated, effectively passing the one-time trigger as a parameter to the graph's
   trigger.

  - state-graph - The graph we are walking
  - state-stack - The current state stack
  - transition - The edge to follow in the graph
  - param - Parameter that will be held with the new state in the state stack
  - one-time-trigger - Appended to any trigger on the edge of the graph, forms a re-frame event to trigger now

  Returns: the new state stack
  "
  [state-graph state-stack transition param one-time-trigger]
  (let [top-state-and- (peek state-stack)
        top-state (if (vector? top-state-and-) (first top-state-and-) top-state-and-)
        [action new-state trigger] (get-in state-graph [top-state transition])
        new-stack (or (next-stack state-stack action new-state param)
                      (do (js/alert (str "Error: Invalid FSM action!\nSaw " transition " from " top-state))
                          state-stack))
        full-trigger (when (or trigger one-time-trigger)
                       (utils/vconcat trigger one-time-trigger))]
    ;; (console :log "PAGE TRANSITION: " state-stack transition new-stack)
    (when-not (empty? full-trigger)
      ;; (console :log "-> TRIGGERING: " full-trigger)
      (>evt full-trigger))
    new-stack))


(defn add-transition
  "Extend the FSM with a new/replaced transition"
  [state-graph from-state transition result]
  (assoc-in state-graph [from-state transition] result))


(defn in-state?
  "Test if a state is active."
  [state-stack state]
  (-> #{state}
      (some (map first state-stack))
      boolean))

(defn at-state?
  "Test if a state is the top active state."
  [state-stack state]
  (= state (first (last state-stack))))
