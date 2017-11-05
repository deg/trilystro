;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.fsm-graph
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [specviz.graphviz :as graphviz]
   [trilystro.fsm-lib :as fsm-lib]))


;;; FSM graphing. Code heavily borrowed from
;;; https://github.com/jebberjeb/fsmviz/blob/master/src/fsmviz/core.cljc


(s/def ::fsm-tuple (s/tuple any? any? any? any?))
(s/def ::fsm-tuples (s/coll-of ::fsm-tuple))
(s/def ::fsm-transition-map (s/nilable (s/map-of any? any?)))
(s/def ::fsm-map (s/map-of any? ::fsm-transition-map))
(s/def ::fsm (s/or :tuples ::fsm-tuples
                   :map ::fsm-map))

(defn- nodes-pushing-into
  "Return the set of all nodes that push to a node.
  This is an attempt to graph the effects of pop. It is not complete, since it ignores
  potential non-structured usages such as {:push a; :shift b; :pop}. But, to be fair:
  1) I don't have any usages of shift while a state is pushed
  2) They would violate 50 years of good taste in structured programming
  3) It may have been a mistake to even support this usage in next-stack."
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

;(def xxx @(re-frame/subscribe [:trilystro.fsm/page-states]))

(defn- map->tuples
  "Returns a collection of [from via to] tuples representing the FSM."
  [state-map]
  (mapcat (fn [[from m]]
            (map (fn [[trans [type to]]]
                   (let [to (if (= type :pop)
                              (first (nodes-pushing-into state-map from))
                              to)]
                     [from trans to type]))
                 m))
          state-map))

(defn- third [coll] (nth coll 2))
(defn- fourth [coll] (nth coll 2))

(defn- start-states
  "Returns states which have no inbound transitions."
  [tuples]
  (set/difference (set (map first tuples)) (set (map third tuples))))

(defn- term-states
  "Returns states which have no outbound transitions."
  [tuples]
  (set/difference (set (map third tuples)) (set (map first tuples))))

(defn clean-name
  "Sanitize a name, per `graphviz/name`, and if the state's name is nil,
  assume it is a start state."
  [state]
  (if state (graphviz/clean-name state) "start"))

(defn tuples->graphviz
  [tuples]
  (console :log "TUPLES: " tuples)
  (concat
   (mapcat (fn [[from via to type]]
             [{::graphviz/from (clean-name from)
               ::graphviz/label (str (case type
                                       :push "PUSH "
                                       :pop "POP "
                                       "")
                                     via)
               ::graphviz/to (clean-name to)}])
           tuples)

   ;; Style initial states
   [{::graphviz/name "start"
     ::graphviz/label ""
     ::graphviz/height 0.25
     ::graphviz/width 0.25
     ::graphviz/shape "circle"
     ::graphviz/style "filled"
     ::graphviz/fillcolor "#000000"}]

   ;; Style terminal states
   (mapv (fn [state]
           {::graphviz/name (clean-name state)
            ::graphviz/shape "doublecircle"})
         (term-states tuples))) )

(defmulti fsm->graphviz* first)

(defmethod fsm->graphviz* :tuples
  [[_ fsm-tuples]]
  (tuples->graphviz fsm-tuples))

(defmethod fsm->graphviz* :map
  [[_ fsm-map]]
  (-> fsm-map
      map->tuples
      tuples->graphviz))

(defn fsm->graphviz
  "Returns a collection of Graphviz elements representing the `fsm`."
  [fsm]
  (let [conformed (s/conform ::fsm fsm)]
    (if (= conformed :clojure.spec/invalid)
      (do (println (s/explain ::fsm fsm))
          [{::graphviz/name "Error"}])
      (fsm->graphviz* conformed))))

(s/fdef generate-image :args (s/cat :state-data ::fsm))

;; TODO this won't handle cursors
 (defn transform-js-data
     [state-data]
     (mapv (fn [row]
             (mapv (fn [x] (if (string? x)
                             (keyword x)
                             x))
                   row))
           (js->clj state-data)))

(defn ^:export generate-image
  "Creates <filename>.svg, using the state map provided.

  `state-data` a map of state -> transition map, or a colletion of
               [from via to] triples."
  [state-data]
  (-> state-data
      fsm->graphviz
      graphviz/dot-string
      (graphviz/generate-image! "")))


;;; ================================================================
;;; ====================   End of fsmviz code  =====================
;;; ================================================================


(defn- node-name
  "For some reason, fsmviz loses hyphens in node names"
  [key]
  (str/replace (name key) #"-" "_"))


(defn render-graph
  "generate-image returns the SVG as an XML string with an embedded explicit width
  and height. Here we use an ugly hack to strip this info, and then embed it into
  hiccup for reagent."
  [graph]
  [:div {:dangerouslySetInnerHTML
         {:__html (-> graph
                      (generate-image)
                      (str/replace-first #"width=\"\d*pt\""  "")
                      (str/replace-first #"height=\"\d*pt\"" ""))}}])
