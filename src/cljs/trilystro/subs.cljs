;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require    [clojure.set :as set]
               [clojure.string :as str]
               [clojure.spec.alpha :as s]
               [re-frame.core :as re-frame]
               [re-frame.loggers :refer [console]]
               [sodium.re-utils :as re-utils :refer [sub2 <sub]]
               [sodium.utils :as utils]
               [trilystro.events :as events]
               [trilystro.firebase :as fb]
               [trilystro.fsm :as fsm]))

(sub2 :git-commit [:git-commit])
(sub2 :name       [:name])
(sub2 :uid        [:user :uid])
(sub2 :user       [:user])


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
              (or (empty? match-set)
                  (not (empty? intersection)))))))

(defn filter-all-tags
  "Filter function that selects lystros whose tags include all
  of the match-set"
  [match-set]
  {:pre [(utils/validate (s/nilable set?) match-set)]}
  (filter (fn [{:keys [tags]}]
            (if (empty? match-set)
              true
              (= (set/intersection match-set (set tags))
                 match-set)))))

(defn filter-url-field
  "Filter function that selects lystros whose url field includes match-url."
  [match-url]
  {:pre [(utils/validate string? match-url)]}
  (filter (fn [lystro]
            (if (empty? match-url)
              (:url lystro)
              (utils/ci-includes? (:url lystro) match-url)))))

(defn filter-text-field
  "Filter function that selects lystros whose text field includes
  match-text or if the text is found in the tags or url, when them
  matching option has been passed in."
  [match-text tags-as-text? url-as-text?]
  {:pre [(utils/validate string? match-text)]}
  (filter (fn [lystro]
            (if (empty? match-text)
              (:text lystro)
              (let [all-text
                    (str (when tags-as-text?
                           (->> (:tags lystro) (interpose " ") (apply str)))
                         " "
                         (when url-as-text?
                           (:url lystro))
                         " "
                         (:text lystro))]
                (utils/ci-includes? all-text match-text))))))


(defn filter-lystros [lystros {:keys [tags-mode tags url text tags-as-text? url-as-text?]}]
  (transduce (comp (if (= tags-mode :all-of)
                     (filter-all-tags tags)
                     (filter-some-tags tags))
                   (filter-url-field (str/trim (or url "")))
                   (filter-text-field (str/trim (or text "")) tags-as-text? url-as-text?))
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
 :user-settings
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path (fb/private-fb-path [:user-settings])}]))
 (fn [settings _]
   settings))

(re-frame/reg-sub
 :users-details
 (fn [_ _]
   ;; [TODO][ch94] Rename :user-details to :users-details
   (re-frame/subscribe [:firebase/on-value {:path (fb/all-shared-fb-path [:user-details])}]))
 (fn [details _]
   details))



(re-frame/reg-sub
 :user-of-id
 (fn [_ _] (re-frame/subscribe [:users-details]))
 (fn [details [_ user-id]]
   (when user-id
     ((keyword user-id) details))))

(re-frame/reg-sub
 :user-pretty-name
 (fn [[_ id]]
   (re-frame/subscribe [:user-of-id id]))
 (fn [user [_ _]]
   (or (:display-name user)
       (:email user))))



(defn- lystros-with-id [lystros-tree]
  (map->vec-of-val+key lystros-tree :firebase-id))

(defn- setify-tags [lystros]
  (reduce-kv (fn [m k v]
               (assoc m k (update v :tags set)))
             {} lystros))

(defn cleanup-lystros
  "Convert group of Lystros from internal Firebase format to proper form"
  [raw-lystros]
  (-> raw-lystros
      setify-tags
      lystros-with-id))

(re-frame/reg-sub
 :lystros
 (fn [_ _]
   [(re-frame/subscribe [:firebase/on-value {:path (fb/private-fb-path [:lystros])}])
    (re-frame/subscribe [:firebase/on-value {:path (fb/all-shared-fb-path [:lystros])}])])
 (fn [[private-lystros shared-lystros] [_ {:keys [tags-mode tags url text tags-as-text? url-as-text?] :as options}] _]
   (into (filter-lystros (cleanup-lystros private-lystros) options)
         (mapcat #(filter-lystros (cleanup-lystros %) options)
                 (vals shared-lystros)))))

(re-frame/reg-sub
 :all-tags
 (fn [_ _] (re-frame/subscribe [:lystros]))
 (fn [lystros]
   (into #{} (mapcat :tags lystros))))


(re-frame/reg-sub
 :tag-counts
 (fn [_ _] (re-frame/subscribe [:lystros]))
 (fn [lystros]
   (frequencies (mapcat :tags lystros))))

(re-frame/reg-sub
 :max-tag-count
 (fn [_ _] (re-frame/subscribe [:tag-counts]))
 (fn [counts _]
   (apply max (vals counts))))

(re-frame/reg-sub
 :tag-class-by-frequency
 (fn [_ _]
   [(re-frame/subscribe [:tag-counts])
    (re-frame/subscribe [:max-tag-count])])
 (fn [[counts max-count] [_ tag]]
   (let [count (counts tag)]
     (cond (<= count 1) "rare-tag"
           (<= count (/ max-count 2)) "average-tag"
           :else "common-tag"))))


(re-frame/reg-sub
 :new-tags
 (fn [_ [_ tags]]
   (let [old-tags (<sub [:all-tags])]
     (clojure.set/difference (set tags) (set old-tags)))))
