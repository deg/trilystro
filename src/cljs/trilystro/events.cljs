;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.events
  (:require
   [cljs-time.coerce :as time-coerce]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [day8.re-frame.http-fx]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [vimsical.re-frame.cofx.inject :as inject]
   [sodium.chrome-utils :as chrome]
   [trilystro.db :as db]
   [trilystro.firebase :as fb]
   [trilystro.fsm :as fsm]))

(s/check-asserts true)

(defn cache-git-commit [db git-desc]
  (assoc db :git-commit git-desc))

(re-frame/reg-event-db
 :got-git-describe
 [db/check-spec-interceptor]
 (fn [db [_ raw-desc]]
   (let [[commit date] (str/split raw-desc #"[\n\r]")]
     (cache-git-commit db {:commit commit :date date}))))

(re-frame/reg-event-db
 :no-git-describe
 [db/check-spec-interceptor]
 (fn [db [_ error]]
   (console :log "Failed to get git description: " (:status-text error))
   (cache-git-commit db {:commit "[unknown]" :date "[unknown]"})))


(re-frame/reg-event-db
 :form-state
 [db/check-spec-interceptor]
 (fn [db [_ form-name form-component value]]
   (if form-component
     (assoc-in db `[:forms ~form-name ~@form-component] value)
     (assoc-in db `[:forms ~form-name] value))))


(re-frame/reg-event-fx
 :commit-lystro
 [db/check-spec-interceptor]
 (fn [{db :db} [_ {:keys [firebase-id tags url text owner public?]}]]
   (assoc
    (fb/fb-event {:db db
                  :for-multi? false
                  :effect-type (if firebase-id
                                 :firebase/write
                                 :firebase/push)
                  :access (if public? :shared :private)
                  :path (if firebase-id
                          [:lystros firebase-id]
                          [:lystros])
                  :value {:tags tags
                          :url url
                          :text text
                          :owner owner
                          :public? public?
                          :timestamp fb/timestamp-marker}})
    :dispatch [::fb/commit-user-setting :default-public? public?])))


(re-frame/reg-event-fx
 :clear-lystro
 [(re-frame/inject-cofx ::inject/sub [::fb/uid])
  db/check-spec-interceptor]
 (fn [{db :db uid ::fb/uid} [_ {:keys [firebase-id owner public?]} :as lystro]]
   (let [mine? (= owner uid)]
     (when mine?
       (fb/fb-event {:for-multi? false
                     :effect-type :firebase/write
                     :db db
                     :access (if public? :shared :private)
                     :path [:lystros firebase-id]
                     :value nil})))))
