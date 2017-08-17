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
   [trilystro.db :as db]))

(s/check-asserts true)

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(def time-format (time-format/formatters :date-time))

(re-frame/reg-event-db
 :db-write
 (fn [db [_ raw-datum]]
   (let [now (time-format/unparse time-format (time/now))
         datum {:time now :data raw-datum}]
     (prn "Will write: " datum)
     #_
     (fb.database/conj! fb-db (str datum)))
   db))
