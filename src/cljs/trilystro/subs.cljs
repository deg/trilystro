;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(defn sub2
  "Shorthand for simple 'layer 2` usage of re-sub"
  [key db-path]
  (re-frame/reg-sub
   key
   (fn [db _] (get-in db db-path))))

(sub2 :name     [:name])
(sub2 :page     [:page])
(sub2 :server   [:server])
(sub2 :user     [:user])
(sub2 :uid      [:user :uid])
(sub2 :new-keys [:new-keys])

(re-frame/reg-sub
 :form-state
 (fn [db [_ form form-component]]
   (get-in db `[:forms ~form ~@form-component])))
