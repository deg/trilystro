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
 :firebase/current-user
 (fn [db _]
   (:firebase/current-user db)))
