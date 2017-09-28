;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.events
  (:require
   [cljs-time.coerce :as time-coerce]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.chrome-utils :as chrome]
   [sodium.re-utils :as re-utils :refer [<sub]]
   [trilystro.db :as db]
   [trilystro.firebase :as fb]
   [trilystro.fsm :as fsm]))

(s/check-asserts true)

(defn- next-page [db transition]
  (update db :page-state fsm/transit-state transition))

(re-frame/reg-event-db
 :page
 (fn [db [_ transition]]
   (next-page db transition)))


(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   (next-page db/default-db :initialize-db)))

(re-frame/reg-event-db
 :form-state
 (fn [db [_ form-name form-component value]]
   (if form-component
     (assoc-in db `[:forms ~form-name ~@form-component] value)
     (assoc-in db `[:forms ~form-name] value))))

(re-frame/reg-event-fx
 :set-user
 (fn [{db :db} [_ user]]
   (into {:db (next-page (assoc db :user user)
                         (if user :login-confirmed :logout))}
         (when user
           {:firebase/write {:path       (fb/private-fb-path [:user-details] (:uid user))
                             :value      (select-keys user [:display-name :email :photo-url])
                             :on-success #(console :log "Logged in:" (:display-name user))
                             :on-failure #(console :error "Failure: " %)}}))))

(defn new-tags [tags]
  (let [old-tags (vals (<sub [:firebase/on-value {:path [:public :tags]}]))]
    (into [] (clojure.set/difference (set tags) (set old-tags)))))

(re-frame/reg-event-fx
 :commit-lystro
 (fn [{db :db} [_ form-key]]
   (let [form-path [:forms form-key]
         form-vals (get-in db form-path)
         {:keys [tags url text]} form-vals]
     {:firebase/multi (conj (mapv #(fb/fb-event {:for-multi? true
                                                 :effect-type :firebase/push
                                                 :db db
                                                 :public? true
                                                 :path [:tags]
                                                 :value %})
                                  (new-tags tags))
                            (let [options {:for-multi? true
                                           :db db
                                           :public? false
                                           :value {:tags tags :url url :text text}} ]
                              (if-let [old-id (:firebase-id form-vals)]
                                (fb/fb-event (assoc options
                                                    :effect-type :firebase/write
                                                    :path [:items old-id]))
                                (fb/fb-event (assoc options
                                                    :effect-type :firebase/push
                                                    :path [:items])))))
      :db (assoc-in db form-path nil)})))

(re-frame/reg-event-fx
 :clear-lystro
 (fn [{db :db} [_ id]]
   (fb/fb-event {:for-multi? false
                 :effect-type :firebase/write
                 :db db
                 :public? false
                 :path [:items id]
                 :value nil})))


(defn set-conj [set new]
  (conj (or set #{}) new))

(re-frame/reg-event-db
 :add-new-tag
 (fn [db [_ form-key]]
   (let [form-path [:forms form-key]
         new-tag (get-in db (conj form-path :new-tag))]
     (-> db
         (update    :new-tags                  set-conj new-tag)
         (update-in (conj form-path :tags)     set-conj new-tag)
         (assoc-in  (conj form-path :new-tag)  "")))))

