;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.events
  (:require
   [ajax.core :as ajax]
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

(re-frame/reg-event-fx
 :initialize-db
 (fn  [_ _]
   {:db db/default-db
    :http-xhrio {:method :get
                 :uri "/git-describe.txt"
                 :params {:cachebuster (str (rand))}
                 :response-format (ajax/text-response-format)
                 :on-success [:got-git-describe]
                 :on-failure [:no-git-describe]}}))

(defn cache-git-commit [db git-desc]
  (assoc db :git-commit git-desc))

(re-frame/reg-event-db
 :got-git-describe
 (fn [db [_ raw-desc]]
   (let [[commit date] (str/split raw-desc #"[\n\r]")]
     (cache-git-commit db {:commit commit :date date}))))

(re-frame/reg-event-db
 :no-git-describe
 (fn [db [_ error]]
   (console :log "Failed to get git description: " error)
   (cache-git-commit db "[unavailable]")))


(re-frame/reg-event-db
 :form-state
 (fn [db [_ form-name form-component value]]
   (if form-component
     (assoc-in db `[:forms ~form-name ~@form-component] value)
     (assoc-in db `[:forms ~form-name] value))))

(re-frame/reg-event-fx
 :set-user
 (fn [{db :db} [_ user]]
   (into {:db (assoc db :user user)
          :dispatch [::fsm/goto (if user :login-confirmed :logout)]}
         (when user
           ;; [TODO][ch94] Should remain :user-details to :users-details in Firebase.
           ;;    Requires a migration hack.
           {:firebase/write {:path       (fb/my-shared-fb-path [:user-details] (:uid user))
                             :value      (select-keys user [:display-name :email :photo-url])
                             :on-success #(console :log "Logged in:" (:display-name user))
                             :on-failure #(console :error "Login failure: " %)}}))))

(re-frame/reg-event-fx
 :commit-lystro
 (fn [{db :db} [_ {:keys [firebase-id tags url text owner public?]}]]
   (let [base-options {:db db
                       :for-multi? true
                       :effect-type :firebase/write}]
     {:firebase/multi [(fb/fb-event (into base-options
                                          {:access :private
                                           :path [:user-settings :default-public?]
                                           :value public?}))
                       (let [lystro-options (into base-options
                                                  {:access (if public? :shared :private)
                                                   :value {:tags tags
                                                           :url url
                                                           :text text
                                                           :owner owner
                                                           :public? public?}})]
                         (if-let [old-id firebase-id]
                           (fb/fb-event (assoc lystro-options
                                               :path [:lystros old-id]))
                           (fb/fb-event (assoc lystro-options
                                               :effect-type :firebase/push
                                               :path [:lystros]))))]})))

(re-frame/reg-event-fx
 :clear-lystro
 [(re-frame/inject-cofx ::inject/sub [:uid])]
 (fn [{db :db uid :uid} [_ {:keys [firebase-id owner public?]} :as lystro]]
   (let [mine? (= owner uid)]
     (when mine?
       (fb/fb-event {:for-multi? false
                     :effect-type :firebase/write
                     :db db
                     :access (if public? :shared :private)
                     :path [:lystros firebase-id]
                     :value nil})))))
