;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require    [clojure.set :as set]
               [clojure.string :as str]
               [clojure.spec.alpha :as s]
               [re-frame.core :as re-frame]
               [re-frame.loggers :refer [console]]
               [sodium.utils :as utils]
               [trilystro.events :as events]
               [trilystro.firebase :as fb]
               [trilystro.fsm :as fsm]))

(defn sub2
  "Shorthand for simple 'layer 2` usage of re-sub"
  [key db-path]
  (re-frame/reg-sub
   key
   (fn [db _] (get-in db db-path))))

(sub2 :name       [:name])
(sub2 :page-state [:page-state])
(sub2 :user       [:user])
(sub2 :uid        [:user :uid])
(sub2 :new-tags   [:new-tags])

(re-frame/reg-sub
 :in-page
 (fn [db [_ page]]
   (fsm/in-state? (:page-state db) page)))

(re-frame/reg-sub
 :form-state
 (fn [db [_ form form-component]]
   (get-in db `[:forms ~form ~@form-component])))



(defn filter-some-tags
  "Filter function that selects lystros whose tags include at least
  one of the match-set"
  [match-set]
  {:pre [(utils/validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [tags]}]
            (let [tags (set tags)
                  intersection (if (empty? match-set)
                                 tags
                                 (set/intersection tags match-set))]
              (not (empty? intersection))))))

(defn filter-all-tags
  "Filter function that selects lystros whose tags include all
  of the match-set"
  [match-set]
  {:pre [(utils/validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [tags]}]
            (if (empty? match-set)
              (boolean tags)
              (= (set/intersection match-set (set tags))
                 match-set)))))

(defn filter-text-field
  "Filter function that selects lystros whose text or url field
  includes match-text."
  [field match-text]
  {:pre [(utils/validate (s/nilable string?) match-text)]}
  (filter (fn [lystro]
            (let [text (or (field lystro) "")]
              (if (empty? match-text)
                text
                (re-find (js/RegExp. match-text "i") text))))))


(defn filter-lystros [lystros {:keys [tags-mode tags url text] :as options}]
  (transduce (comp (if (= tags-mode :all-of)
                     (filter-all-tags tags)
                     (filter-some-tags tags))
                   (filter-text-field :url url)
                   (filter-text-field :text text))
             conj
             lystros))

(defn map->vec-of-val+key
  "Convert a map of maps into a vector of maps that include the original key as a value
  e.g.:
  (map->vec-of-val+key {:x1 {:a 1} :x2 {:a 2} :x3 {:a 3}} :id)
  => [{:a 1 :id :x1} {:a 2 :id :x2} {:a 3 :id :x3}]
  "
  [vals-map key-key]
  (reduce-kv
   (fn [coll k v]
     (conj coll (assoc v key-key k)))
   []
   vals-map))

(re-frame/reg-sub
 :lystros
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path (fb/private-fb-path [:items])}]))
 (fn [all-lystros [_ {:keys [tags-mode tags url text] :as options}] _]
   (filter-lystros (map->vec-of-val+key all-lystros :firebase-id) options)))


(re-frame/reg-sub
 :all-tags
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path (fb/public-fb-path [:tags])}]))
 (fn [tag-map _]
   (map name (keys tag-map))))
