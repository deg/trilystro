;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :page
 (fn [db _]
   (:page db)))

(re-frame/reg-sub
 :server
 (fn [db _]
   (:server db)))

(re-frame/reg-sub
 :user
 (fn [db _]
   (:user db)))

(re-frame/reg-sub
 :uid
 (fn [db _]
   (get-in db [:user :uid])))


