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
   [iron.re-utils :refer [>evt]]
   [trilystro.db :as db]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]))

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


(defn lystro-commit-event [db {:keys [firebase-id tags url text owner public? original-public?] :as lystro}]
  (let [exists? (some? firebase-id)
        changed-access? (and exists?
                             (some? original-public?)
                             (not (= original-public? public?)))]
    (fb/fb-event {:db db
                  :for-multi? false
                  :effect-type (if exists? :firebase/write :firebase/push)
                  :path (if exists? [:lystros firebase-id] [:lystros])
                  :access (if public? :shared :private)
                  :on-success #(when changed-access?
                                 ;; Private and public lystros are stored in different branches of the
                                 ;; Firebase tree. So, if the access has changed, we need to explicitly
                                 ;; delete the old one. To be safe, we only do so here, on success of
                                 ;; writing the new one.
                                 ;; [TODO][ch153] This whole thing should really be moved into a Firebase
                                 ;; transaction. We are not protected against the broader set of problems
                                 ;; that two clients might be modifying the same Lystro simultaneously
                                 ;; so we need to check for any changes before writing. If done right,
                                 ;; the access stuff should fall out nicely. See
                                 ;; https://stackoverflow.com/questions/42183179/firebase-atmonic-add-and-delete-entry
                                 ;; and
                                 ;; https://firebase.google.com/docs/database/web/read-and-write#save_data_as_transactions
                                 (>evt [:clear-lystro (assoc lystro :public? original-public?)]))
                  :value {:tags tags
                          :url url
                          :text text
                          :owner owner
                          :public? public?
                          :timestamp fb/timestamp-marker}})))

(re-frame/reg-event-fx
 :commit-lystro
 [db/check-spec-interceptor]
 (fn [{db :db} [_ {:keys [firebase-id tags url text owner public? original-public?] :as lystro}]]
   (assoc
    (lystro-commit-event db lystro)
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
