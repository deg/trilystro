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
               [trilystro.events :as events]))

(defn sub2
  "Shorthand for simple 'layer 2` usage of re-sub"
  [key db-path]
  (re-frame/reg-sub
   key
   (fn [db _] (get-in db db-path))))

(sub2 :name     [:name])
(sub2 :page     [:page])
(sub2 :server   [:server])
(sub2 :user     [:user])
(sub2 :uid      [:user :uid])
(sub2 :new-keys [:new-keys])

(re-frame/reg-sub
 :form-state
 (fn [db [_ form form-component]]
   (get-in db `[:forms ~form ~@form-component])))



(defn filter-some-keys [match-set]
  {:pre [(utils/validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [keys]}]
            (let [keys (set keys)
                  intersection (if (empty? match-set)
                                 keys
                                 (set/intersection keys match-set))]
              (not (empty? intersection))))))

(defn filter-all-keys [match-set]
  {:pre [(utils/validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [keys]}]
            (if (empty? match-set)
              (boolean keys)
              (= (set/intersection match-set (set keys))
                 match-set)))))

(defn filter-text-field [field match-text]
  {:pre [(utils/validate (s/nilable string?) match-text)]}
  (filter (fn [lystro]
            (let [text (or (field lystro) "")]
              (if (empty? match-text)
                text
                (str/includes? text match-text))))))


(defn filter-lystros [lystros {:keys [keys-mode keys url text]}]
  (transduce (comp (if (= keys-mode :all-of)
                     (filter-all-keys keys)
                     (filter-some-keys keys))
                   (filter-text-field :url url)
                   (filter-text-field :text text))
             conj
             lystros))

(re-frame/reg-sub
 :lystros
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path (events/private-fb-path [:items])}]))
 (fn [all-lystros [_ {:keys [keys-mode keys url text] :as options}] _]
   (filter-lystros (vals all-lystros) options)))
