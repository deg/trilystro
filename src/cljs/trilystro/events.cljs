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
   [sodium.chrome-utils :as chrome]
   [trilystro.db :as db]
   [trilystro.firebase :as firebase]))

(s/check-asserts true)

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :page
 (fn [db [_ page]]
   (assoc db :page page)))

(re-frame/reg-event-db
 :firebase/current-user
 (fn [db [_ user]]
   (assoc db :firebase/current-user user)))

(re-frame/reg-event-fx
 :sign-in
 (fn [_ _]
   {:firebase/google-sign-in nil}))

(re-frame/reg-event-fx
 :sign-out
 (fn [_ _]
   {:firebase/sign-out nil}))

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

