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

