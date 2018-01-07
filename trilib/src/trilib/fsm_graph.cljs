;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilib.fsm-graph
  (:require
   [clojure.string :as str]
   [fsmviz.core :as fsmviz]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [trilib.fsm-lib :as fsm-lib]))


;;; Graphing. (This is still a bit hackish, as it works around the existing fsmviz rather than modifying it).

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


(defn- node-name
  "For some reason, fsmviz loses hyphens in node names"
  [key]
  (keyword (str/replace (name key) #"-" "_")))


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
