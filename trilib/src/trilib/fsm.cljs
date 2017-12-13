;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilib.fsm
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [fsmviz.core :as fsmviz]
   [iron.re-utils :refer [sub2]]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [trilib.fsm-lib :as fsm-lib]))

;;; Trilystro app states (login status and page transitions)
;;; Note: The state graph is in db.cljs

(s/def ::page-state (s/tuple keyword? any?))
(s/def ::page-state-stack (s/coll-of :trilib.fsm/page-state))

(s/def ::db-keys (s/keys :req [::page-graph ::page-state-stack]))


(sub2 ::page-graph [::page-graph])

;;; Are we currently in a page (either directly or in a modal upon it)
(re-frame/reg-sub
 ::in-page?
 (fn [db [_ page]]
   (fsm-lib/in-state? (::page-state-stack db) page)))

(defn goto
  "This is mostly an internal function, used by the ::goto event. But, it
  is exposed for those occassional times (e.g. startup) that is is useful
  to do a synchronous transition."
  [db transition {:keys [param dispatch]}]
  (update db ::page-state-stack
           (partial fsm-lib/transit-state! (::page-graph db))
           transition param dispatch))

;;; Goto a new app state
;;; - param - Store this parameter in the new state
;;; - dispatch - Trigger this re-frame event after the state transition
(re-frame/reg-event-db
 ::goto
 ;;[db/check-spec-interceptor]  ;; [TODO] Untangle deps, so we can call this here
 (fn [db [_ transition {:keys [param dispatch]}]]
   (goto db transition {:param param :dispatch dispatch})))


(defn- page-param [state-stack]
  (-> state-stack last second))

(defn- update-page-param [db fcn & args]
  ;; [TODO] This cries out for learning and using Specter (https://github.com/nathanmarz/specter)
  (let [old-param (-> db ::page-state-stack last second)
        new-param (apply fcn old-param args)
        new-final-state [(-> db ::page-state-stack last first) new-param]]
    (assoc db ::page-state-stack (conj (-> db ::page-state-stack pop) new-final-state))))

(re-frame/reg-sub
 ::page-param
 (fn [db _]
   (-> db ::page-state-stack page-param)))

(re-frame/reg-event-db
 ::update-page-param
 ;;[db/check-spec-interceptor]  ;; [TODO] Untangle deps, so we can call this here
 (fn [db [_ fcn & args]]
   (apply update-page-param db fcn args)))

(re-frame/reg-sub
 ::page-param-val
 (fn [db [_ val-key]]
   (-> db ::page-state-stack page-param val-key)))

(re-frame/reg-event-db
 ::update-page-param-val
 ;;[db/check-spec-interceptor]  ;; [TODO] Untangle deps, so we can call this here
 (fn [db [_ key val]]
   (update-page-param db #(assoc %1 key %2) val)))


