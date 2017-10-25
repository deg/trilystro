;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.fsm
  (:require
   [clojure.string :as str]
   [fsmviz.core :as fsmviz]
   [sodium.re-utils :as re-utils :refer [sub2]]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [trilystro.fsm-lib :as fsm-lib]))

;;; Trilystro app states (login status and page transitions)
;;; Note: The state graph is in db.cljs

(sub2 ::page-states [::page-states])

;;; Are we currently in a page (either directly or in a modal upon it)
(re-frame/reg-sub
 ::in-page?
 (fn [db [_ page]]
   (fsm-lib/in-state? (::page-state db) page)))


;;; Goto a new app state
;;; - param - Store this parameter in the new state
;;; - dispatch - Trigger this re-frame event after the state transition
(re-frame/reg-event-db
 ::goto
 (fn [db [_ transition {:keys [param dispatch]}]]
   (update db ::page-state
           (partial fsm-lib/transit-state! (::page-states db))
           transition param dispatch)))


(defn- page-param [state-stack]
  (-> state-stack last second))

(defn- update-page-param [db fcn & args]
  ;; [TODO] This cries out for learning and using Specter (https://github.com/nathanmarz/specter)
  (let [old-param (-> db ::page-state last second)
        new-param (apply fcn old-param args)
        new-final-state [(-> db ::page-state last first) new-param]]
    (assoc db ::page-state (conj (-> db ::page-state pop) new-final-state))))

(re-frame/reg-sub
 ::page-param
 (fn [db _]
   (-> db ::page-state page-param)))

(re-frame/reg-event-db
 ::update-page-param
 (fn [db [_ fcn & args]]
   (apply update-page-param db fcn args)))

(re-frame/reg-sub
 ::page-param-val
 (fn [db [_ val-key]]
   (-> db ::page-state page-param val-key)))

(re-frame/reg-event-db
 ::update-page-param-val
 (fn [db [_ key val]]
   (update-page-param db #(assoc %1 key %2) val)))


