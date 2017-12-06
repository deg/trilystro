(ns vuagain.chromex.popup.handlers
  (:require [re-frame.core :as re-frame]
            [re-frame.loggers :refer [console]]
            [vuagain.chromex.popup.db :as db]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
 :set-user
 (fn [db [_ user]]
   (assoc db :user user)))

(re-frame/reg-event-db
 :set-background-port
 (fn [db [_ background-port]]
   (assoc db :background-port background-port)))

(re-frame/reg-event-db
 :set-current-tab
 (fn [db [_ current-tab]]
   (assoc db :current-tab current-tab)))


(re-frame/reg-event-db
 :update-page-param-val
 (fn [db [_ param val]]
   (console :log "Setting " param " to " val)
   (assoc-in db [:page-params param] val)
   db))

(re-frame/reg-event-db
 :commit-lystro
 (fn [db [_ lystro]]
   (console :log "COMMITTING: --- :" lystro)
   db))
